package com.vd.canary.obmp.aggregate;

import com.vd.canary.core.bo.ResponseBO;
import com.vd.canary.obmp.aggregate.annotation.DataAggregatePropertyBind;
import com.vd.canary.obmp.aggregate.annotation.DataAggregateType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author zx
 * @date 2020/12/16 14:13
 */
@Slf4j
@Aspect
@Component
public class DataAggregateAOP {

    private ConcurrentHashMap<String, AggregateTargetNode> AggregateTargetMap = new ConcurrentHashMap();
    //todo 线程安全,享元
    private ConcurrentHashMap<Class<?>, AggregateSourceNode> AggregateSourceMap = new ConcurrentHashMap<>();

    public static PropertyDescriptor findTar(Class<?> beanClass, String name) {
        PropertyDescriptor[] properties = PropertyUtils.getPropertyDescriptors(beanClass);
        for (PropertyDescriptor property : properties) {
            //大小写敏感
            if (property.getName().equals(name)) {
                return property;
            }
        }

        log.error("数据聚合-无法获取Bean对应属性的读写方法,class={},property={}", beanClass.getName(), name);
        throw new RuntimeException("数据聚合-无法获取Bean对应属性的getter/setter");
    }

    @Pointcut("bean(*Controller)")

    public void resultAop() {
    }

    /***
     *
     * @param pjp
     * @return java.lang.Object
     * @author zhengxin
     */
    @Around("resultAop()")
    @SneakyThrows
    public Object resultAround(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        Object proceed = pjp.proceed();
        if (proceed instanceof ResponseBO) {
            ResponseBO response = (ResponseBO) proceed;
            //todo response<List类型返回值>支持
            Object responseData = response.getData();
            Class<?> clazz = responseData.getClass();
            if (clazz.isAnnotationPresent(DataAggregateType.class)) {
                if (!AggregateTargetMap.containsKey(clazz.getName())) {
                    AggregateTargetNode aggregateTargetNode = parsingClass(new StringBuffer(), clazz, responseData, new AggregateTargetNode());
                    aggregateTargetNode.initAggregateNode();
                    AggregateTargetMap.put(clazz.getName(), aggregateTargetNode);
                }
                //实例化执行器注入依赖值
                //todo wapper?
                AggregateTargetNode targetNode = AggregateTargetMap.get(clazz.getName());

                //通过理论可访问路径构建实际可访问路径
                List<String> actualPathList = new ArrayList<>();
                List<String> ignoreList = new ArrayList<>();
                for (String theoryPath : targetNode.propertyList) {
                    if (filterIgnore(ignoreList, theoryPath)) { continue;}
                    List buildStatementList = buildStatementList(responseData, new ArrayList(), theoryPath, "", "write", ignoreList);
                    actualPathList.addAll(buildStatementList);
                }
                if (actualPathList.size() == 0) {
                    return proceed;
                }

                //todo 复杂情况的值绑定状态,如resp.xx+resp.list.xx绑定到同一个执行器
                for (Map.Entry<String, List<AggregateSourceNode>> targetPropertyEntry : targetNode.propertyAggregateMap.entrySet()) {
                    for (AggregateSourceNode sourceNode : targetPropertyEntry.getValue()) {
                        List<OrderDataAggregate> instances = new ArrayList(Arrays.asList((OrderDataAggregate) sourceNode.sourceClass.getDeclaredConstructor().newInstance()));
                        Map<String, List<String>> classMap = targetNode.bindPropertyMap.get(sourceNode.sourceClass.getName());
                        Map<String, List<String>> defaultClassMap = targetNode.bindPropertyMap.get("DEFAULT_CLASS_NAME");

                        for (Map.Entry<String, PropertyDescriptor> entry : sourceNode.propertyAggregateMap.entrySet()) {
                            String k = entry.getKey();
                            Method writeMethod = entry.getValue().getWriteMethod();
                            List<String> buildStatementList;
                            if (classMap != null && classMap.containsKey(k)) {
                                buildStatementList = classMap.get(k);
                            } else {
                                buildStatementList = defaultClassMap == null ? null : defaultClassMap.get(k);
                            }

                            //注入依赖值
                            //先不考虑执行器的属性绑定跨层的情况
                            if (buildStatementList != null) {
                                int size = buildStatementList.size();
                                if (buildStatementList.size() > 1) {
                                    if (instances.size() == 1) {
                                        //todo 深clone方式
                                        for (int i = 1; i < size; i++) {
                                            OrderDataAggregate orderDataAggregate = (OrderDataAggregate) sourceNode.sourceClass.getDeclaredConstructor().newInstance();
                                            instances.add(orderDataAggregate);
                                        }
                                    }
                                    for (int i = 0; i < size; i++) {
                                        String statementIndex = buildStatementList.get(i);
                                        OrderDataAggregate orderDataAggregateIndex = instances.get(i);
                                        Object propertyValue = PropertyUtils.getProperty(responseData, statementIndex);
                                        //todo 参数类型不匹配的情况
                                        writeMethod.invoke(orderDataAggregateIndex, propertyValue);
                                    }
                                } else {
                                    Object propertyValue = PropertyUtils.getProperty(responseData, buildStatementList.get(0));
                                    writeMethod.invoke(instances.get(0), propertyValue);
                                }
                            }
                        }

                        for (int i = 0; i < instances.size(); i++) {
                            OrderDataAggregate dataAggregate = instances.get(i);
                            //执行聚合方法 todo 代理
                            dataAggregate.doDataAggregate();

                            //数据反写
                            for (String allow : sourceNode.allowPropertyList) {
                                List<String> tarStatementList = findTarStatementList(actualPathList, targetPropertyEntry.getKey(), allow);
                                if (tarStatementList.size() == 0) {
                                    continue;
                                }
                                PropertyDescriptor propertyDescriptor = sourceNode.propertyAggregateMap.get(allow);
                                Object val = propertyDescriptor.getReadMethod().invoke(dataAggregate);
                                String filterTarStatement = filterTarStatementList(tarStatementList, i);

                                //issue:lombok@Accessors(chain = true)注解生成的set方法无法被此工具类识别
                                //throw NoSuchMethodException
                                PropertyUtils.setProperty(responseData, filterTarStatement, val);
                            }
                        }
                    }
                }
            }
        }
        return proceed;
    }

    //todo 获取注解了bind的路径属性名
    //获取
    private AggregateTargetNode parsingClass(StringBuffer absolutePathName, Class<?> clazz, Object resultObj, AggregateTargetNode aggregateTargetNode) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        for (Field field : clazz.getDeclaredFields()) {
            //忽略static属性
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Class<?> propertyTypeClass = field.getType();
            String nextPathName = absolutePathName.toString() + "." + field.getName();
            nextPathName = nextPathName.charAt(0) == '.' ? nextPathName.substring(1) : nextPathName;

            aggregateTargetNode.propertyList.add(nextPathName);

            if (propertyTypeClass.isAssignableFrom(List.class)) {
                //todo List多重泛型嵌套测试
                ParameterizedType pType = (ParameterizedType) field.getGenericType();
                if (pType.getActualTypeArguments().length > 0) {
                    propertyTypeClass = Class.forName(pType.getActualTypeArguments()[0].getTypeName());
                }
            }

            //解析聚合属性绑定注解
            if (field.isAnnotationPresent(DataAggregatePropertyBind.class)) {
                String bindName = field.getAnnotation(DataAggregatePropertyBind.class).value();

                //构建取值执行语句
                //todo 这里有问题,取值语句在第一次解析class时构建,后续的对象值发生变化后就不适用了(放到line67可行吗?)
                List buildStatementList = buildStatementList(resultObj, new ArrayList(), nextPathName, "", "read", new ArrayList<>());
                //todo 绑定执行器的相对属性名如重复可以为类名.属性名
                String bindSourceClassName = "DEFAULT_CLASS_NAME";
                String bindSourcePropertyName;
                int index = bindName.indexOf(".");
                if (index > 0) {
                    //类名.属性名的情况
                    bindSourceClassName = bindName.substring(0, index - 1);
                    bindSourcePropertyName = bindName.substring(index + 1);
                } else if (index == 0) {
                    bindSourcePropertyName = bindName.substring(1);
                } else {
                    //仅有属性名的情况
                    bindSourcePropertyName = bindName;
                }

                if (aggregateTargetNode.bindPropertyMap.containsKey(bindSourceClassName)) {
                    Map<String, List<String>> listMap = aggregateTargetNode.bindPropertyMap.get(bindSourceClassName);
                    if (listMap.get(bindSourcePropertyName) != null) {
                        throw new RuntimeException("数据聚合-聚合对象中绑定执行器属性重复");
                    }
                    aggregateTargetNode.bindPropertyMap.get(bindSourceClassName).put(bindSourcePropertyName, buildStatementList);
                } else {
                    Map<String, List<String>> listMap = new HashMap<>();
                    listMap.put(bindSourcePropertyName, buildStatementList);
                    aggregateTargetNode.bindPropertyMap.put(bindSourceClassName, listMap);
                }
            }

            if (isParsingClass(propertyTypeClass)) {
                if (propertyTypeClass.isAnnotationPresent(DataAggregateType.class)) {
                    Class[] sourceClass = propertyTypeClass.getAnnotation(DataAggregateType.class).value();
                    if (sourceClass.length == 0) {
                        //根据全限定类名加载class
                        List<Class> sourceClassList = new ArrayList();
                        String[] classNames = propertyTypeClass.getAnnotation(DataAggregateType.class).classNames();
                        if (classNames.length > 0) {
                            int i = 0;
                            try {
                                for (; i < classNames.length; i++) {
                                    sourceClassList.add(Class.forName(classNames[i]));
                                }
                            } catch (ClassNotFoundException classNotFoundException) {
                                log.error("无法为聚合对象加载指定执行器-ClassNotFoundException,聚合对象={},执行器className={}",propertyTypeClass.getName(),classNames[i]);
                                throw new RuntimeException("无法为聚合对象加载指定执行器");
                            }
                            sourceClass = sourceClassList.toArray(new Class[sourceClassList.size()]);
                        }
                    }
                    if (sourceClass.length > 0) {
                        //解析聚合执行器注解
                        for (Class source : sourceClass) {
                            if (!AggregateSourceMap.containsKey(source.getName())) {
                                AggregateSourceNode sourceNode = new AggregateSourceNode(source);

                                //解析@Transient
                                for (Field sourceField : source.getDeclaredFields()) {
                                    if (!sourceField.isAnnotationPresent(org.springframework.data.annotation.Transient.class)) {
                                        sourceNode.allowPropertyList.add(sourceField.getName());
                                    }
                                    sourceNode.propertyAggregateMap.put(sourceField.getName(), findTar(source, sourceField.getName()));
                                }
                                AggregateSourceMap.put(source, sourceNode);
                            }

                            //设置解析对象待执行器
                            if (!aggregateTargetNode.propertyAggregateMap.containsKey(nextPathName)) {
                                aggregateTargetNode.propertyAggregateMap.put(nextPathName, new ArrayList<>(Arrays.asList(AggregateSourceMap.get(source))));
                            } else {
                                aggregateTargetNode.propertyAggregateMap.get(nextPathName).add(AggregateSourceMap.get(source));
                            }
                        }
                    }
                }

                parsingClass(new StringBuffer(absolutePathName.toString() + "." + field.getName()), propertyTypeClass, resultObj,
                        aggregateTargetNode);
            }
        }

        return aggregateTargetNode;
    }

    private boolean isParsingClass(Class<?> clazz) {
        return clazz.getPackageName().contains("com.vd");
    }

    /**
     * 给定对象与对象的任意属性路径,返回当前对象下该属性路径的所有实际可访问路径
     * @param source        当前取出对象
     * @param statementList 构建语句List
     * @param nextPath      下一层路径
     * @param transmitPath 标示类型为list属性的数组下标
     * @return java.util.List
     */
    private List buildStatementList(Object source, List statementList, String nextPath, String transmitPath, String Mode, List<String> ignoreList) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        //todo 多层list还没测
        if ("".equals(nextPath)) {
            return statementList;
        }
        int index = nextPath.indexOf(".");
        String curPath;
        if (index > 0) {
            curPath = nextPath.substring(0, index);
            nextPath = nextPath.substring(index);
        } else if (index == 0) {
            curPath = nextPath.substring(1);
            nextPath = "";
        } else {
            curPath = nextPath;
            nextPath = "";
        }
        String finalPath = transmitPath + "." + curPath;
        finalPath = finalPath.charAt(0) == '.' ? finalPath.substring(1) : finalPath;

        Object propertyValue = null;
        try {
            propertyValue = PropertyUtils.getProperty(source, curPath);
        } catch (NoSuchMethodException e) {
            //注:Bean拷贝后类型为List的属性的实际类型会变成source中相同属性名的类型而不是target中对应属性名泛型指定的类型
            //issue 1.理论上存在该属性访问路径,实际上访问不到 2.理论无-实际有的情况无需考虑
            //写模式加入到路径中(属性在最外层(或嵌套属性的最外层)的情况)
            if (Mode.equals("write")) {
                statementList.add(finalPath);
                //嵌套属性最外层为null时里层属性直接忽略,防止重复报错
                ignoreList.add(finalPath);
                return statementList;
            }
        } catch (NestedNullException e) {
            //多重嵌套属性访问里层属性时外层属性为空
            return statementList;
        }
        if (propertyValue == null) {
            if (Mode.equals("write")) {
                statementList.add(finalPath);
            }
            return statementList;
        }

        if (propertyValue instanceof List) {
            List sourceList = (List) propertyValue;
            for (int i = 0; i < sourceList.size(); i++) {
                Object o = sourceList.get(i);
                String s = curPath + "[" + i + "]";

                buildStatementList(o, statementList, nextPath, s, Mode, ignoreList);
            }
        } else {
            if (isParsingClass(propertyValue.getClass())) {
                buildStatementList(propertyValue, statementList, nextPath, transmitPath, Mode, ignoreList);
            } else {
                statementList.add(finalPath);
            }
        }
        return statementList;
    }

    private List<String> findTarStatementList(List<String> actualPathList, String targetProperty, String sourceProperty) {
        List<String> tarStatementList = new ArrayList<>();
        if (targetProperty.equals(sourceProperty)) {
            //执行器中属性总为非嵌套,相等直接返回
            tarStatementList.add(actualPathList.get(actualPathList.indexOf(targetProperty)));
            return tarStatementList;
        }

        tarStatementList.addAll(actualPathList.stream().filter(actualPath -> {
            //过滤掉中括号以及里面的值
            if (actualPath.replaceAll("\\[.*?\\]", "").contains(targetProperty)) {
                return true;
            }
            return false;
        }).collect(Collectors.toList()).stream().filter(actualPath -> {
            String[] split = actualPath.split("\\.");
            if (split.length == 0) {
                if (actualPath.equals(sourceProperty)) {
                    return true;
                }
            } else if (split[split.length - 1].equals(sourceProperty)) {
                return true;
            }
            return false;
        }).collect(Collectors.toList()));
        return tarStatementList;
    }

    private String filterTarStatementList(List<String> actualTarStatementList, int index) {
        if (actualTarStatementList.size() == 1) {
            return actualTarStatementList.get(0);
        }
        for (String val : actualTarStatementList) {
            int indexStart = val.indexOf("[");
            int indexEnd = val.indexOf("]");
            if (String.valueOf(index).equals(val.substring(indexStart + 1, indexEnd))) {
                return val;
            }
        }
        throw new RuntimeException("数据聚合-异常");
    }

    private boolean filterIgnore(List<String> ignoreList, String curProperty) {
        for (String ignore : ignoreList) {
            if (curProperty.contains(ignore)) {
                return true;
            }
        }

        return false;
    }

    static class AggregateTargetNode {
        //Class中所有属性平铺路径List(理论上可访问的属性)
        final List<String> propertyList = new ArrayList<>();

        //需要执行属性执行器map
        //key 相对属性名 val 属性执行器对应AggregateSourceNode
        //todo 弱引用map
        final Map<String, List<AggregateSourceNode>> propertyAggregateMap = new HashMap<>();

        //key 属性执行器类名 val <key 绑定的属性执行器中的相对属性名 val 取值执行语句>
        final Map<String, Map<String, List<String>>> bindPropertyMap = new HashMap<>();

        public AggregateTargetNode() {
        }

        public void initAggregateNode() {

        }


    }

    static class AggregateSourceNode {
        final Class<?> sourceClass;
        final List<String> allowPropertyList = new ArrayList<>();
        //key  自身相对属性名 val 读写方法
        //todo 支持绑定对象重载?
        //执行器属性名,读写对象map
        final Map<String, PropertyDescriptor> propertyAggregateMap = new HashMap<>();
        //

        public AggregateSourceNode(Class<?> sourceClass) {
            this.sourceClass = sourceClass;
        }

    }
}
