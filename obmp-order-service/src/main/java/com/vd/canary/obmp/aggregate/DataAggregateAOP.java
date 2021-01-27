package com.vd.canary.obmp.aggregate;

import cn.hutool.json.JSONUtil;
import com.vd.canary.core.bo.ResponseBO;
import com.vd.canary.core.bo.ResponsePageBO;
import com.vd.canary.core.exception.BusinessException;
import com.vd.canary.obmp.aggregate.annotation.DataAggregatePropertyBind;
import com.vd.canary.obmp.aggregate.annotation.DataAggregatePropertyMapping;
import com.vd.canary.obmp.aggregate.annotation.DataAggregateType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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

    @Resource
    ApplicationContext applicationContext;

    private ConcurrentHashMap<String, AggregateTargetNode> AggregateTargetMap = new ConcurrentHashMap();
    //todo 线程安全,享元
    private ConcurrentHashMap<Class<?>, AggregateSourceNode> AggregateSourceMap = new ConcurrentHashMap<>();

    @Pointcut("bean(*Controller) && @annotation(com.vd.canary.obmp.aggregate.annotation.DataAggregate)")
    public void resultAop() {
    }

    /*@Around("resultAop()")
    @SneakyThrows
    public Object resultAround(ProceedingJoinPoint pjp) {
        Object responseData = null;
        Object proceed = pjp.proceed();
        return proceed;
    }*/

    /**
     * 数据聚合AOP
     * 1.聚合对象Class静态属性解析
     * 2.聚合对象动态值解析
     * 3.执行器属性动态绑定
     * 4.执行器执行
     * 5.执行器执行结果反写至聚合对象
     *
     * @param response
     * @return void
     * @author zhengxin
     */
    @SneakyThrows
    @AfterReturning(pointcut = "resultAop()", returning = "response")
    public void doDataAggregate(Object response) {
        //todo 测试后删除
        log.info("数据聚合(beta日志)-response对象={}", JSONUtil.toJsonStr(response));
        Object responseData = null;

        /* 支持ResponsePageBO<resp>,
         * ResponseBO<List<resp>>,ResponseBO<resp>结构返回值
         * 及上述类型的resp中任意对象、任意结构的多重嵌套(容器仅限util.List和所有位于com.vd包下的对象)
         */
        if (response instanceof ResponseBO) {
            responseData = ((ResponseBO) response).getData();
        } else if (response instanceof ResponsePageBO) {
            ResponsePageBO responsePageBO = (ResponsePageBO) response;
            if (responsePageBO.getData() != null && responsePageBO.getData().getList() != null && responsePageBO.getData().getList().size() > 0) {
                responseData = responsePageBO.getData().getList();
            }
        }
        if (responseData == null) {
            return;
        }

        Class<?> clazz = getAuthenticClass(responseData);
        if (clazz == null) {
            return;
        }
        if (!AggregateTargetMap.containsKey(clazz.getName())) {
            AggregateTargetNode aggregateTargetNode;
            try {
                aggregateTargetNode = parsingClass(new StringBuffer(), clazz, new AggregateTargetNode(), null, "");
            } catch (ClassNotFoundException e) {
                log.error("数据聚合-解析静态Class异常", e);
                throw new BusinessException(120_000, "数据聚合-ClassNotFound:解析静态Class异常");
            }
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
                List<AbstractOrderDataAggregate> instances = new ArrayList();
                Map<String, List<AggregateTargetBindProperty>> classMap = targetNode.bindPropertyMap.get(sourceNode.sourceClass.getName());
                Map<String, List<AggregateTargetBindProperty>> defaultClassMap = targetNode.bindPropertyMap.get("DEFAULT_CLASS_NAME");

                //遍历执行器属性,如果属性在聚合对象中存在绑定关系,则从聚合对象中获取对应的值注入到执行器中的属性
                for (Map.Entry<String, PropertyDescriptor> entry : sourceNode.propertyAggregateMap.entrySet()) {
                    String sourcePropertyName = entry.getKey();

                    if (sourceNode.resourcePropertyMap.containsKey(sourcePropertyName)) {
                        //该属性需要从spring中获取
                        continue;
                    }
                    Method writeMethod = entry.getValue().getWriteMethod();
                    List<String> buildStatementList = null;
                    AggregateTargetBindProperty tarProperty;

                    //从聚合对象解析关系中获取对应属性的属性平铺路径(理论访问路径)
                    tarProperty = targetNode.getTarBindProperty(classMap, sourcePropertyName, 0);
                    tarProperty = tarProperty == null ? targetNode.getTarBindProperty(defaultClassMap, sourcePropertyName, 0) : tarProperty;

                    if (tarProperty != null && tarProperty.getAggregateTargetPropertyName() != null) {
                        if (!sourceNode.isSingleton() && tarProperty.getBindType().equals(DataAggregatePropertyBind.BindType.MANY_TO_ONE)) {
                            //设置执行器对应关系为多对一
                            sourceNode.setSingleton(true);
                        }

                        //从属性理论访问路径构建实际访问路径
                        buildStatementList = buildStatementList(responseData, new ArrayList(), tarProperty.getAggregateTargetPropertyName(), "", "", "", "read", new ArrayList<>());
                    }
                    //todo 执行器的属性绑定跨层时,先一后多的情况下有问题 -多个属性注解指定多对一时,出现顺序先后问题
                    //注入依赖值
                    if (buildStatementList != null && buildStatementList.size() > 0) {
                        if (instances.size() == 0) {
                            instances.add(getOrderDataAggregateInstance(sourceNode));
                        }

                        int size = buildStatementList.size();
                        //从多原则
                        //聚合对象与执行器1:1与n:n的情况
                        if (size > 1) {
                            if (!sourceNode.isSingleton()) {
                                //此时执行器必定不为单例
                                log.error("数据聚合-聚合对象与执行器对应关系异常,");
                                throw new BusinessException(120_000, "数据聚合-聚合对象与执行器对应关系异常");
                            }
                            if (instances.size() == 1) {
                                //todo 深clone方式
                                for (int i = 1; i < size; i++) {
                                    instances.add(getOrderDataAggregateInstance(sourceNode));
                                }
                            }
                            for (int i = 0; i < size; i++) {
                                setActuatorProperty(responseData, writeMethod, tarProperty, buildStatementList.get(i), instances.get(i));
                            }
                        } else {
                            setActuatorProperty(responseData, writeMethod, tarProperty, buildStatementList.get(0), instances.get(0));
                        }
                    }
                }

                for (int i = 0; i < instances.size(); i++) {
                    AbstractOrderDataAggregate dataAggregate = instances.get(i);
                    //执行聚合方法 todo 代理
                    if (!dataAggregate.isActuatorFlag()) {
                        continue;
                    }
                    dataAggregate.doDataAggregate();

                    //数据反写
                    for (String waitWriteVal : sourceNode.allowPropertyList) {
                        //在聚合对象中查找属性对应的访问路径
                        //@DataAggregateType注解所在层级优先
                        //~表示根路径
                        String possiblePath = curTargetPropertyName.equals("~") ? waitWriteVal : curTargetPropertyName + "." + waitWriteVal;
                        //todo 1.可以在read模式时一起返回write,做区分,这样只用调用一次 2.当对象为List时相同属性的实际路径只用解析一次(当前解析n次)
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
    }

    private void setActuatorProperty(Object responseData, Method writeMethod, AggregateTargetBindProperty tarProperty, String statementIndex, AbstractOrderDataAggregate orderDataAggregateIndex) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object propertyValue = PropertyUtils.getProperty(responseData, statementIndex);
        if (propertyValue == null) {
            if (tarProperty.isRequired()) {
                //执行器中该属性为必要
                orderDataAggregateIndex.setActuatorFlag(false);
            }
        } else {
            //todo 参数类型不匹配的情况
            writeMethod.invoke(orderDataAggregateIndex, propertyValue);
        }
    }

    private boolean isParsingClass(Class<?> clazz) {
        return clazz.getPackageName().contains("com.vd");
    }

    private boolean isIgnoreField(Field field) {
        //忽略static属性
        if (Modifier.isStatic(field.getModifiers())) {
            return true;
        }

        //忽略lombok日志注入
        if (field.getName().equals("log")) {
            return true;
        }

        return false;
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
            //聚合对象中List属性对象长度为0的情况(自行初始化或执行器反写)
            if (sourceList.size() == 0 && !statementList.contains(finalPath) && Mode.equals("write")) {
                statementList.add(finalPath);
            }
            for (int i = 0; i < sourceList.size(); i++) {
                Object o = sourceList.get(i);
                if (isParsingClass(o.getClass())) {
                    /* 1.0.0_beta-issue1 聚合对象中为List的属性第一次在propertyValue == null被放入statementList
                     *  第二次进来因已被赋值且nextPath=""不会被放入statementList
                     *  */
                    if (!nextPath.equals("")) {
                        String suffix = "[" + i + "]";

                        transferPrefixIndex = "";
                        buildStatementList(o, statementList, nextPath, transferPrefixIndex, suffix, finalPath, Mode, ignoreList);
                    } else {
                        if (!statementList.contains(finalPath)) {
                            statementList.add(finalPath);
                        }
                    }
                }
            }
        } else {
            if (isParsingClass(propertyValue.getClass()) && !nextPath.equals("")) {
                buildStatementList(propertyValue, statementList, nextPath, transferPrefixIndex, transferSuffixIndex, finalPath, Mode, ignoreList);
            } else {
                if (!statementList.contains(finalPath)) {
                    statementList.add(finalPath);
                }
            }
        }
        return statementList;
    }

    /**
     * 解析静态Class对象(递归)
     * 约定:聚合对象中属性为自定义类或容器时,为其指定执行器需将
     * 执行器放到属性对应的类名之上(容器为泛型对应的类名)
     * @param absolutePathName
     * @param clazz
     * @param targetNode
     * @return
     * @throws ClassNotFoundException
     * @author zhengxin
     */
    //todo 按照执行器,聚合对象,以及分别对应的注解build模式重写?
    private AggregateTargetNode parsingClass(StringBuffer absolutePathName, Class<?> clazz, AggregateTargetNode targetNode, AggregateSourceNode sourceNode, String type) throws ClassNotFoundException {
        String targetPropertyName = absolutePathName.toString().equals("") ? "~" : absolutePathName.toString();
        targetPropertyName = targetPropertyName.charAt(0) == '.' ? targetPropertyName.substring(1) : targetPropertyName;

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

            for (Class source : sourceClass) {
                if (!AggregateSourceMap.containsKey(source.getName())) {
                    AggregateSourceNode sourceNodeNow = new AggregateSourceNode(source);
                    parsingClass(new StringBuffer(), source, targetNode, sourceNodeNow, "actuator");
                    AggregateSourceMap.put(source, sourceNodeNow);
                }

                //设置解析对象待执行器
                if (!targetNode.propertyAggregateMap.containsKey(targetPropertyName)) {
                    targetNode.propertyAggregateMap.put(targetPropertyName, new ArrayList<>(Arrays.asList(AggregateSourceMap.get(source).clone())));
                } else {
                    targetNode.propertyAggregateMap.get(targetPropertyName).add(AggregateSourceMap.get(source).clone());
                }
            }
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (isIgnoreField(field)) {
                continue;
            }

            Class<?> propertyTypeClass = field.getType();
            String nextPathName = absolutePathName.toString() + "." + field.getName();
            nextPathName = nextPathName.charAt(0) == '.' ? nextPathName.substring(1) : nextPathName;

            if (propertyTypeClass.isAssignableFrom(List.class)) {
                //todo List多重泛型嵌套测试
                ParameterizedType pType = (ParameterizedType) field.getGenericType();
                if (pType.getActualTypeArguments().length > 0) {
                    propertyTypeClass = Class.forName(pType.getActualTypeArguments()[0].getTypeName());
                }
            }

            if (type.equals("actuator")) {
                //解析执行器属性
                if (field.isAnnotationPresent(org.springframework.data.annotation.Transient.class)) {
                    //解析@Transient
                    sourceNode.ignorePropertyList.add(nextPathName);
                } else if (field.isAnnotationPresent(javax.annotation.Resource.class)) {
                    Map<String, ?> beans = applicationContext.getBeansOfType(field.getType());
                    Object springBean;
                    try {
                        //todo 存在多个实现时用@quir注解直接指定名字注入的情况
                        springBean = beans.entrySet().iterator().next().getValue();
                    } catch (Exception e) {
                        log.error("数据聚合-解析执行器自动注入属性失败,执行器={},属性={}", clazz.getName(), field.getName());
                        throw new BusinessException(120_000, "数据聚合-解析执行器自动注入属性失败");
                    }

                    if (springBean != null) {
                        sourceNode.resourcePropertyMap.put(field.getName(), springBean);
                    }
                } else {
                    String finalNextPathName = nextPathName;
                    if (!sourceNode.ignorePropertyList.stream().anyMatch(ignore -> finalNextPathName.contains(ignore))) {
                        sourceNode.allowPropertyList.add(nextPathName);
                    }
                }
                sourceNode.propertyAggregateMap.put(nextPathName, findTar(clazz, field.getName()));
            } else {
                //解析聚合对象属性绑定注解
                targetNode.propertyList.add(nextPathName);

                String bindSourcePropertyName = "";
                String bindSourceClassName = "DEFAULT_CLASS_NAME";
                AggregateTargetBindProperty aggregateTargetBindProperty = null;
                if (field.isAnnotationPresent(DataAggregatePropertyBind.class)) {
                    DataAggregatePropertyBind propertyBind = field.getAnnotation(DataAggregatePropertyBind.class);
                    String bindName = propertyBind.value();
                    //todo 绑定执行器的相对属性名如重复可以为类名.属性名,如果存在同名class如何处理(全限定类名不好截断)
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

                    if (propertyBind.type().equals(DataAggregatePropertyBind.BindType.MANY_TO_ONE)) {
                        //只要有一个属性指定就可以(运行时解析或整个完成解析后再执行)
                        for (AggregateSourceNode aggregateSourceNode : targetNode.propertyAggregateMap.get(targetPropertyName)) {
                            if (!aggregateSourceNode.isSingleton()) {
                                for (Map.Entry<String, PropertyDescriptor> aggregateSourceNodeEntry : aggregateSourceNode.propertyAggregateMap.entrySet()) {
                                    if (aggregateSourceNodeEntry.getKey().equals(bindName)) {
                                        aggregateSourceNode.setSingleton(true);
                                    }
                                }
                            }
                        }
                        //设置执行器对应关系为多对一
                        //支持执行器对应关系随聚合对象变化
                        sourceNode.setSingleton(true);
                    }
                    aggregateTargetBindProperty = new AggregateTargetBindProperty(bindSourcePropertyName, nextPathName, propertyBind.required(), propertyBind.type(), 0);
                } else if (field.isAnnotationPresent(DataAggregatePropertyMapping.class)) {
                    DataAggregatePropertyMapping propertyMapping = field.getAnnotation(DataAggregatePropertyMapping.class);
                    String value = propertyMapping.value();
                    String classNameStr = propertyMapping.classNameStr();
                    Class<?> mappingClass = propertyMapping.className();

                    String[] cutOutPath = cutOutPath(value);
                    if (!mappingClass.equals(Object.class)) {
                        bindSourceClassName = mappingClass.getName();
                    } else if (Character.isUpperCase(value.charAt(0))) {
                        value = cutOutPath[1].substring(1);
                        bindSourceClassName = cutOutPath[0];
                    } else if (!"".equals(classNameStr)) {
                        bindSourceClassName = classNameStr;
                    }

                    //todo 1.字段-在聚合对象中查找当前层级的同名字段 2.属性-查找当前层级同名属性 3.剩余字段如何处理?
                    bindSourcePropertyName = value;
                    aggregateTargetBindProperty = new AggregateTargetBindProperty(value, nextPathName, true, DataAggregatePropertyBind.BindType.DEFAULT, 1);
                }

                if (!bindSourcePropertyName.equals("") && aggregateTargetBindProperty != null) {
                    if (targetNode.bindPropertyMap.containsKey(bindSourceClassName)) {
                        Map<String, List<AggregateTargetBindProperty>> listMap = targetNode.bindPropertyMap.get(bindSourceClassName);
                        if (targetNode.getTarBindProperty(listMap, bindSourcePropertyName, 0) != null) {
                            log.error("数据聚合-解析聚合对象异常,执行器属性重复绑定,class={},property={}", clazz.getName(), nextPathName);
                            throw new BusinessException(120_000, "数据聚合-聚合对象中绑定执行器属性重复");
                        }
                        if (listMap.get(bindSourcePropertyName) != null) {
                            targetNode.bindPropertyMap.get(bindSourceClassName).get(bindSourcePropertyName).add(aggregateTargetBindProperty);
                        } else {
                            targetNode.bindPropertyMap.get(bindSourceClassName).put(bindSourcePropertyName, new ArrayList<>(List.of(aggregateTargetBindProperty)));
                        }
                    } else {
                        Map<String, List<AggregateTargetBindProperty>> listMap = new HashMap<>();
                        listMap.put(bindSourcePropertyName, new ArrayList<>(List.of(aggregateTargetBindProperty)));
                        targetNode.bindPropertyMap.put(bindSourceClassName, listMap);
                    }
                }
            }
            if (isParsingClass(propertyTypeClass)) {
                parsingClass(new StringBuffer(absolutePathName.toString() + "." + field.getName()), propertyTypeClass, targetNode, sourceNode, type);
            }
        }

        return targetNode;
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
            //todo 现在存在嵌套
            //执行器中属性总为非嵌套,相等直接返回
            tarStatementList.add(actualPathList.get(actualPathList.indexOf(targetProperty)));
            return tarStatementList;
        }

        tarStatementList.addAll(actualPathList.stream().filter(actualPath -> {
            //过滤中括号以及里面的值
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

    public PropertyDescriptor findTar(Class<?> beanClass, String name) {
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

    private AbstractOrderDataAggregate getOrderDataAggregateInstance(AggregateSourceNode sourceNode) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        AbstractOrderDataAggregate orderDataAggregate = (AbstractOrderDataAggregate) sourceNode.sourceClass.getDeclaredConstructor().newInstance();
        Map<Method, Object> initMap = new HashMap<>();
        for (Map.Entry<String, Object> classEntry : sourceNode.resourcePropertyMap.entrySet()) {
            initMap.put(sourceNode.propertyAggregateMap.get(classEntry.getKey()).getWriteMethod(), classEntry.getValue());
        }
        orderDataAggregate.init(initMap);
        return orderDataAggregate;
    }

//    private AggregateSourceNode getTarSourceNode(Class clazz, String className) {
//        //todo classname可能为全限定
//        if (clazz != null) {
//            return AggregateSourceMap.get(clazz);
//        }
//
//        if (className != null) {
//            for (Map.Entry<Class<?>, AggregateSourceNode> nodeEntry : AggregateSourceMap.entrySet()) {
//                if (nodeEntry.getKey().getSimpleName().equals(className)) {
//                    return nodeEntry.getValue();
//                }
//            }
//        }
//
//        log.error("数据聚合-执行器获取异常,class={},className={}", clazz.getName(), className);
//        throw new BusinessException(120_000, "数据聚合-执行器获取异常");
//    }

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
        //key 聚合对象的相对属性名 val 对应执行器节点解析信息
        //todo 弱引用map
        final Map<String, List<AggregateSourceNode>> propertyAggregateMap = new HashMap<>();

        //key 执行器类名(或default) val <key 绑定/映射的执行器中的相对属性名 val 聚合对象相对属性名>
        final Map<String, Map<String, List<AggregateTargetBindProperty>>> bindPropertyMap = new HashMap<>();

        public AggregateTargetBindProperty getTarBindProperty(Map<String, List<AggregateTargetBindProperty>> map, String sourcePropertyName, int type) {
            AggregateTargetBindProperty targetBindProperty = null;
            if (map != null && map.containsKey(sourcePropertyName)) {
                Optional<AggregateTargetBindProperty> targetProperty = map.get(sourcePropertyName).stream().filter(tarProperty -> {
                    if (tarProperty.nodeType == type) {
                        return true;
                    }
                    return false;
                }).findFirst();
                if (targetProperty.isPresent()) {
                    targetBindProperty = targetProperty.get();
                }
            }

            return targetBindProperty;
        }

        public AggregateTargetNode() {
        }

        public void initAggregateNode() {
        }
    }

    static class AggregateSourceNode implements Cloneable {
        /* 标记执行器是否单例 */
        boolean singleton = false;
        final Class<?> sourceClass;
        final List<String> ignorePropertyList = new ArrayList<>();
        final List<String> allowPropertyList = new ArrayList<>();
        //key  自身相对属性名 val 读写方法
        //todo 支持绑定对象重载?
        //执行器属性名,读写对象map
        //todo 需要知道每个属性的类型(是否为list)
        final Map<String, PropertyDescriptor> propertyAggregateMap = new HashMap<>();
        //key 自身相对属性名 val spring托管Bean
        //执行器中需要注入spring托管对象Map
        final Map<String, Object> resourcePropertyMap = new HashMap<>();

        public AggregateSourceNode(Class<?> sourceClass) {
            this.sourceClass = sourceClass;
        }

        public boolean isSingleton() {
            return singleton;
        }

        public void setSingleton(boolean singleton) {
            this.singleton = singleton;
        }

        protected AggregateSourceNode clone() {
            try {
                return (AggregateSourceNode) super.clone();
            } catch (CloneNotSupportedException e) {
                log.error("数据聚合-执行器克隆异常,执行器={}", sourceClass.getName(), e);
                throw new BusinessException(120_000, "数据聚合-执行器克隆异常");
            }
        }
    }

    static class AggregateTargetBindProperty {
        /**
         * 标识节点类别
         * 0-绑定
         * 1-映射
         */
        private final int nodeType;

        private final DataAggregatePropertyBind.BindType bindType;
        //绑定的执行器中的相对属性名
        private final String actuatorPropertyName;
        //聚合对象相对属性名
        private final String aggregateTargetPropertyName;
        //标示绑定到执行器的属性是否必要
        private final boolean required;

        public AggregateTargetBindProperty(String v1, String v2, boolean v3, DataAggregatePropertyBind.BindType v4, int v5) {
            this.actuatorPropertyName = v1;
            this.aggregateTargetPropertyName = v2;
            this.required = v3;
            this.bindType = v4;
            this.nodeType = v5;
        }

        public String getActuatorPropertyName() {
            return actuatorPropertyName;
        }

        public String getAggregateTargetPropertyName() {
            return aggregateTargetPropertyName;
        }

        public boolean isRequired() {
            return required;
        }

        public DataAggregatePropertyBind.BindType getBindType() {
            return bindType;
        }
    }
}
