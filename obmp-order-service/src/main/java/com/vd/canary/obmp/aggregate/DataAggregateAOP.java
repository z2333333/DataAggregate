package com.vd.canary.obmp.aggregate;

import com.vd.canary.core.bo.ResponseBO;
import com.vd.canary.core.bo.ResponsePageBO;
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

    @Pointcut("bean(*Controller) && @annotation(com.vd.canary.obmp.aggregate.annotation.DataAggregate)")

    public void resultAop() {
    }

    /***
     * 数据聚合AOP
     * 1.聚合对象Class静态属性解析
     * 2.聚合对象动态值解析
     * 3.执行器属性动态绑定
     * 4.执行器执行
     * 5.执行器执行结果反写至聚合对象
     *
     * @param pjp
     * @return java.lang.Object
     * @author zhengxin
     */
    @Around("resultAop()")
    @SneakyThrows
    public Object resultAround(ProceedingJoinPoint pjp) {
        if (pjp.proceed() == null) {
            return pjp.proceed();
        }
        Object responseData = null;
        Object proceed = pjp.proceed();

        /* 支持ResponsePageBO<resp>,
         * ResponseBO<List<resp>>,ResponseBO<resp>结构返回值
         * 及上述类型的resp中任意对象、任意结构的多重嵌套(容器仅限util.List和所有位于com.vd包下的对象)
         */
        if (proceed instanceof ResponseBO) {
            responseData = ((ResponseBO) proceed).getData();
        } else if (proceed instanceof ResponsePageBO) {
            ResponsePageBO responsePageBO = (ResponsePageBO) proceed;
            if (responsePageBO.getData() != null && responsePageBO.getData().getList() != null && responsePageBO.getData().getList().size() > 0) {
                responseData = responsePageBO.getData().getList();
            }
        }
        if (responseData == null) {
            return pjp.proceed();
        }
        Class<?> clazz = getAuthenticClass(responseData);
        if (clazz == null) {
            return proceed;
        }
        if (!AggregateTargetMap.containsKey(clazz.getName())) {
            AggregateTargetNode aggregateTargetNode = parsingClass(new StringBuffer(), clazz, new AggregateTargetNode());
            aggregateTargetNode.initAggregateNode();
            AggregateTargetMap.put(clazz.getName(), aggregateTargetNode);
        }
        //实例化执行器注入依赖值
        //todo wapper?
        AggregateTargetNode targetNode = AggregateTargetMap.get(clazz.getName());

        //todo 复杂情况的值绑定状态,如resp.xx+resp.list.xx绑定到同一个执行器
        //遍历聚合对象中绑定了执行器的属性
        for (Map.Entry<String, List<AggregateSourceNode>> targetPropertyEntry : targetNode.propertyAggregateMap.entrySet()) {
            String curTargetPropertyName = targetPropertyEntry.getKey();
            //遍历每个属性绑定的所有执行器
            for (AggregateSourceNode sourceNode : targetPropertyEntry.getValue()) {
                List<OrderDataAggregate> instances = new ArrayList();
                Map<String, String> classMap = targetNode.bindPropertyMap.get(sourceNode.sourceClass.getName());
                Map<String, String> defaultClassMap = targetNode.bindPropertyMap.get("DEFAULT_CLASS_NAME");

                //遍历执行器的属性,如果属性在聚合对象中存在绑定关系,则从聚合对象中获取对应的值注入到执行器中的属性
                for (Map.Entry<String, PropertyDescriptor> entry : sourceNode.propertyAggregateMap.entrySet()) {
                    String sourcePropertyName = entry.getKey();
                    Method writeMethod = entry.getValue().getWriteMethod();
                    List<String> buildStatementList = null;
                    String tarProperty;

                    //从聚合对象解析关系中获取对应属性的属性平铺路径(理论访问路径)
                    if (classMap != null && classMap.containsKey(sourcePropertyName)) {
                        tarProperty = classMap.get(sourcePropertyName);
                    } else {
                        tarProperty = defaultClassMap == null ? null : defaultClassMap.get(sourcePropertyName);
                    }

                    if (tarProperty != null) {
                        //从属性理论访问路径构建实际访问路径
                        buildStatementList = buildStatementList(responseData, new ArrayList(), tarProperty, "", "", "", "read", new ArrayList<>());
                    }
                    //todo 先不考虑执行器的属性绑定跨层的情况
                    //注入依赖值
                    if (buildStatementList != null && buildStatementList.size() > 0) {
                        if (instances.size() == 0) {
                            instances.add((OrderDataAggregate) sourceNode.sourceClass.getDeclaredConstructor().newInstance());
                        }

                        int size = buildStatementList.size();
                        //从多原则
                        //聚合对象与执行器1:1与n:n的情况
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
                    for (String waitWriteVal : sourceNode.allowPropertyList) {
                        //在聚合对象中查找属性对应的访问路径
                        //@DataAggregateType注解所在层级优先
                        //~表示根路径
                        String possiblePath = curTargetPropertyName.equals("~") ? waitWriteVal : curTargetPropertyName + "." + waitWriteVal;
                        //todo 可以在read模式时一起返回write,做区分,这样只用调用一次
                        List<String> targetStatementList = buildStatementList(responseData, new ArrayList(), possiblePath, "", "", "", "write", new ArrayList<>());

                        if (targetStatementList.size() == 0) {
                            //todo 分支未测
                            //完整查找
                            //通过聚合对象理论可访问路径构建实际可访问路径
                            List<String> actualPathList = new ArrayList<>();
                            for (String theoryPath : targetNode.propertyList) {
                                List buildStatementList = buildStatementList(responseData, new ArrayList(), theoryPath, "", "", "", "write", new ArrayList<>());
                                actualPathList.addAll(buildStatementList);
                            }
                            if (actualPathList.size() == 0) {
                                continue;
                            }
                            targetStatementList = findTarStatementList(actualPathList, targetPropertyEntry.getKey(), waitWriteVal);
                        }

                        if (targetStatementList.size() > 0) {
                            PropertyDescriptor propertyDescriptor = sourceNode.propertyAggregateMap.get(waitWriteVal);
                            Object val = propertyDescriptor.getReadMethod().invoke(dataAggregate);
                            //todo 当前适配1:1与n:n
                            //实际可访问路径与执行器的index有序且对应,直接执行
                            //String filterTarStatement = filterTarStatementList(targetStatementList, i);
                            String targetStatement = targetStatementList.get(i);

                            try {
                                PropertyUtils.setProperty(responseData, targetStatement, val);
                            } catch (NoSuchMethodException e) {
                                //抛出该异常的情况
                                //1.issue:lombok@Accessors(chain = true)注解生成的set方法无法被此工具类识别
                                //2.buildStatementList中对write模式的处理(聚合对象中不需要执行器中的某些属性,但write模式中加进来了)
                                //todo 第2点的处理存在矛盾,需重新评估
                                log.error("数据聚合-当前反写的字段不存在,路径={}", targetStatement, e);
                            }
                        }
                    }
                }
            }
        }
        return proceed;
    }

    /**
     * 解析静态Class对象(递归)
     * @param absolutePathName
     * @param clazz
     * @param aggregateTargetNode
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @author zhengxin
     */
    private AggregateTargetNode parsingClass(StringBuffer absolutePathName, Class<?> clazz, AggregateTargetNode aggregateTargetNode) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (clazz.isAnnotationPresent(DataAggregateType.class)) {
            Class[] sourceClass = clazz.getAnnotation(DataAggregateType.class).value();
            if (sourceClass.length == 0) {
                //根据全限定类名加载class
                String[] classNames = clazz.getAnnotation(DataAggregateType.class).classNames();
                if (classNames.length > 0) {
                    List<Class> sourceClassList = new ArrayList();
                    int i = 0;
                    try {
                        for (; i < classNames.length; i++) {
                            sourceClassList.add(Class.forName(classNames[i]));
                        }
                    } catch (ClassNotFoundException classNotFoundException) {
                        log.error("无法为聚合对象加载指定执行器-ClassNotFoundException,聚合对象={},执行器className={}", clazz.getName(), classNames[i]);
                        throw new RuntimeException("无法为聚合对象加载指定执行器");
                    }
                    sourceClass = sourceClassList.toArray(new Class[sourceClassList.size()]);
                }
            }
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

                String targetPropertyName = absolutePathName.toString().equals("") ? "~" : absolutePathName.toString();
                targetPropertyName = targetPropertyName.charAt(0) == '.' ? targetPropertyName.substring(1) : targetPropertyName;
                //设置解析对象待执行器
                if (!aggregateTargetNode.propertyAggregateMap.containsKey(targetPropertyName)) {
                    aggregateTargetNode.propertyAggregateMap.put(targetPropertyName, new ArrayList<>(Arrays.asList(AggregateSourceMap.get(source))));
                } else {
                    aggregateTargetNode.propertyAggregateMap.get(targetPropertyName).add(AggregateSourceMap.get(source));
                }
            }
        }

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
                //List buildStatementList = buildStatementList(resultObj, new ArrayList(), nextPathName, "", "read", new ArrayList<>());
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
                    Map<String, String> listMap = aggregateTargetNode.bindPropertyMap.get(bindSourceClassName);
                    if (listMap.get(bindSourcePropertyName) != null) {
                        throw new RuntimeException("数据聚合-聚合对象中绑定执行器属性重复");
                    }
                    aggregateTargetNode.bindPropertyMap.get(bindSourceClassName).put(bindSourcePropertyName, nextPathName);
                } else {
                    Map<String, String> listMap = new HashMap<>();
                    listMap.put(bindSourcePropertyName, nextPathName);
                    aggregateTargetNode.bindPropertyMap.put(bindSourceClassName, listMap);
                }
            }

            if (isParsingClass(propertyTypeClass)) {
                parsingClass(new StringBuffer(absolutePathName.toString() + "." + field.getName()), propertyTypeClass, aggregateTargetNode);
            }
        }

        return aggregateTargetNode;
    }

    private boolean isParsingClass(Class<?> clazz) {
        return clazz.getPackageName().contains("com.vd");
    }

    /**
     * 聚合对象动态值解析(递归)
     * 给定对象与对象的任意属性路径,返回当前对象该属性路径的实际可访问路径
     *
     * @param source              当前取出对象
     * @param statementList       构建语句List
     * @param nextPath            下一层路径
     * @param transferPrefixIndex 标示source类型为list属性的数组下标
     * @param transferSuffixIndex 标示property类型为list属性的数组下标
     * @return java.util.List
     * @author zhengxin
     */
    //todo 问题:1.多层嵌套属性最外层为空时里层无需遍历 2.多层嵌套属性上层属性不为空时下层无需重新获取最外层
    //todo 多层list时PropertyUtils支持以[0]直接访问最外层属性,[0].[0].xxx
    //todo 优化String
    private List buildStatementList(Object source, List statementList, String nextPath, String transferPrefixIndex, String transferSuffixIndex, String finalPath, String Mode, List<String> ignoreList) throws IllegalAccessException, InvocationTargetException {
        //todo 多层list还没测
        if ("".equals(nextPath)) {
            return statementList;
        }

        //支持List<... Resp>结构的source解析
        if (source instanceof List) {
            List sources = (List) source;
            for (int i = 0; i < sources.size(); i++) {
                Object o = sources.get(i);
                String prefix = "[" + i + "]";

                buildStatementList(o, statementList, nextPath, prefix, transferSuffixIndex, finalPath, Mode, ignoreList);
            }
            return statementList;
        }

        String[] cutOutPath = cutOutPath(nextPath);
        String curPath = cutOutPath[0];
        nextPath = cutOutPath[1];

        finalPath = transferSuffixIndex.equals("") ? finalPath + "." + curPath : finalPath + transferSuffixIndex + "." + curPath;
        finalPath = finalPath.charAt(0) == '.' ? finalPath.substring(1) : finalPath;
        finalPath = transferPrefixIndex.equals("") ? finalPath : transferPrefixIndex + "." + finalPath;

        Object propertyValue = null;
        try {
            //todo 用检测属性的方法提高性能
            propertyValue = PropertyUtils.getProperty(source, curPath);
        } catch (NoSuchMethodException e) {
            //注:Bean拷贝后类型为List的属性的实际类型会变成source中相同属性名的类型而不是target中对应属性名泛型指定的类型
            //issue 1.理论上存在该属性访问路径(parsingClass解析),实际上访问不到(list中的class变了) 2.理论无-实际有的情况无需考虑
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
            statementList.add(finalPath);
            ignoreList.add(finalPath);

            return statementList;
        }

        if (propertyValue instanceof List) {
            List sourceList = (List) propertyValue;
            for (int i = 0; i < sourceList.size(); i++) {
                Object o = sourceList.get(i);
                String suffix = "[" + i + "]";

                transferPrefixIndex = "";
                buildStatementList(o, statementList, nextPath, transferPrefixIndex, suffix, finalPath, Mode, ignoreList);
            }
        } else {
            if (isParsingClass(propertyValue.getClass()) && !nextPath.equals("")) {
                buildStatementList(propertyValue, statementList, nextPath, transferPrefixIndex, transferSuffixIndex, finalPath, Mode, ignoreList);
            } else {
                statementList.add(finalPath);
            }
        }
        return statementList;
    }

    /**
     * 从实际可访问路径中匹配执行器中待反写的属性对应的访问路径
     *
     * @param actualPathList
     * @param targetProperty 聚合对象中绑定执行的属性名
     * @param sourceProperty 执行器中待反写的属性名
     * @return java.util.List<java.lang.String>
     * @author zhengxin
     */
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

    /***
     * 从实际可访问路径中匹配目标属性
     *
     * @param actualTarStatementList
     * @param tarProperty
     * @return java.util.List<java.lang.String>
     * @author zhengxin
     */
    private List<String> filterTarStatementList(List<String> actualTarStatementList, String tarProperty) {
        return actualTarStatementList.stream().filter(actualPath -> {
            //过滤掉中括号以及里面的值
            if (actualPath.replaceAll("\\[.*?\\]", "").equals(tarProperty)) {
                return true;
            } else {
                return false;
            }
        }).collect(Collectors.toList());
    }

    /***
     * 过滤指定数组下标的访问路径
     *
     * @param actualTarStatementList
     * @param index
     * @return java.lang.String
     * @author zhengxin
     */
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

    /**
     * 获取返回对象的真实类型
     * Ex:List<A> return A
     *
     * @param source
     * @return java.lang.Class
     * @author zhengxin
     */
    private Class getAuthenticClass(Object source) {
        if (source instanceof List) {
            while (source instanceof List) {
                source = getFirstListVal((List) source);
                if (source == null) {
                    return null;
                }
            }
        } else if (source instanceof Map) {
            //未支持map
            return null;
        }
        if (!isParsingClass(source.getClass())) {
            return null;
        }
        return source.getClass();
    }

    private Object getFirstListVal(List source) {
        if (source.size() > 0) {
            return source.get(0);
        } else {
            return null;
        }
    }

    /***
     * 截取下一级路径
     *
     * @param nextPath
     * @return void
     * @author zhengxin
     */
    private String[] cutOutPath(String nextPath) {
        String curPath;
        int index = nextPath.indexOf(".");
        if (index > 0) {
            curPath = nextPath.substring(0, index);
            nextPath = nextPath.substring(index);
        } else if (index == 0) {
            curPath = nextPath.substring(1);
            int index1 = curPath.indexOf(".");
            if (index1 > 0) {
                nextPath = curPath.substring(index1);
                curPath = curPath.substring(0, index1);
            } else {
                nextPath = "";
            }
        } else {
            curPath = nextPath;
            nextPath = "";
        }

        return new String[]{curPath, nextPath};
    }

    /**
     * 聚合对象
     */
    static class AggregateTargetNode {
        //Class中所有属性平铺路径List(理论上可访问的属性)
        final List<String> propertyList = new ArrayList<>();

        //需要执行属性执行器map
        //key 聚合对象的相对属性名 val 属性执行器对应AggregateSourceNode
        //todo 弱引用map
        final Map<String, List<AggregateSourceNode>> propertyAggregateMap = new HashMap<>();

        //key 属性执行器类名 val <key 绑定的属性执行器中的相对属性名 val 聚合对象相对属性名>
        final Map<String, Map<String, String>> bindPropertyMap = new HashMap<>();

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
