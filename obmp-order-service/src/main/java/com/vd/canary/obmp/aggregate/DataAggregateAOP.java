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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    //todo 关键性能节点多线程
    @SneakyThrows
    @AfterReturning(pointcut = "resultAop()", returning = "response")
    public void doDataAggregate(Object response) {
        //todo 测试后删除
        log.info("数据聚合(beta日志)-response对象={}", JSONUtil.toJsonStr(response));
        Object responseData = null;

        /* 特性:
         * 1.支持ResponsePageBO<resp>,
         * ResponseBO<List<resp>>,ResponseBO<resp>结构返回值
         * 及上述类型的resp中任意对象、任意结构的多重嵌套(容器仅限util.List和所有位于com.vd包下的对象)
         * 2.支持聚合对象绑定任意多个执行器,及每个执行器属性跨对象、嵌套的绑定
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
                aggregateTargetNode = parsingClass(new StringBuffer(), clazz, new AggregateTargetNode(clazz), null, "", new HashMap());
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
        for (Map.Entry<String, List<AggregatePrepare>> propertyPrepareEntity : targetNode.aggregatePrepareMap.entrySet()) {
            String curTargetPropertyName = propertyPrepareEntity.getKey();

            for (AggregatePrepare aggregatePrepare : propertyPrepareEntity.getValue()) {
                //为执行器描述节点建立当前resp的层级数据
                List<AbstractDataAggregate> instances = buildDataAggregate(responseData, aggregatePrepare);

                Map<String, List<String>> statementCacheMap = new HashMap<>();
                for (int i = 0; i < instances.size(); i++) {
                    AbstractDataAggregate dataAggregate = instances.get(i);
                    //执行聚合方法 todo 代理
                    if (!dataAggregate.isActuatorFlag()) {
                        continue;
                    }
                    dataAggregate.doDataAggregate();

                    //数据反写
                    AggregateSourceNode sourceNode = aggregatePrepare.aggregateSourceNode;
                    for (String waitWriteVal : sourceNode.allowPropertyList) {
                        List<String> targetStatementList;
                        if (!statementCacheMap.containsKey(waitWriteVal)) {
                            //在聚合对象中查找属性对应的访问路径
                            //@DataAggregateType注解所在层级优先
                            //~表示根路径
                            String possiblePath = curTargetPropertyName.equals("~") ? waitWriteVal : curTargetPropertyName + "." + waitWriteVal;
                            //todo 1.可以在read模式时一起返回write,做区分,这样只用调用一次 2.当对象为List时相同属性的实际路径只用解析一次(当前解析n次)
                            targetStatementList = buildStatementList(responseData, new ArrayList(), possiblePath, -1, -1, "", "write", new ArrayList<>(), null);

                            if (targetStatementList.size() == 0) {
                                //完整查找
                                //通过聚合对象理论可访问路径构建实际可访问路径
                                List<String> actualPathList = new ArrayList<>();
                                for (String theoryPath : targetNode.propertyList) {
                                    List buildStatementList = buildStatementList(responseData, new ArrayList(), theoryPath, -1, -1, "", "write", new ArrayList<>(), null);
                                    actualPathList.addAll(buildStatementList);
                                }
                                if (actualPathList.size() == 0) {
                                    continue;
                                }
                                targetStatementList = findTarStatementList(actualPathList, curTargetPropertyName, waitWriteVal);
                            }
                            statementCacheMap.put(waitWriteVal, targetStatementList);
                        } else {
                            targetStatementList = statementCacheMap.get(waitWriteVal);
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
                                //2.buildStatementList中对write模式的处理(聚合对象中不需要执行器中的某些属性,但write模式中加进来了)-已取消
                                log.error("数据聚合-数据反写异常,无法获取属性对应setter方法,路径={}", targetStatement, e);
                            }
                        }
                    }
                }
            }
        }
    }

    private Object setActuatorProperty(Object responseData, Method writeMethod, AggregateBaseNode baseNode, String statementIndex, AbstractDataAggregate orderDataAggregateIndex) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object propertyValue = PropertyUtils.getProperty(responseData, statementIndex);
        if (propertyValue == null && baseNode.isRequired()) {
            //执行器中该属性为必要
            orderDataAggregateIndex.setActuatorFlag(false);
        } else {
            //todo 参数类型不匹配的情况
            writeMethod.invoke(orderDataAggregateIndex, propertyValue);
        }

        return propertyValue;
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
    //todo 优化String(path计算的用node的curLevelPropertyName与commonPrepareNodes.path)
    //todo 递归的性能与溢出隐患问题-尾递归能否解决
    private List buildStatementList(Object source, List statementList, String nextPath, Integer transferPrefixIndex, Integer transferSuffixIndex, String finalPath, String Mode, List<String> ignoreList, Node node) throws IllegalAccessException, InvocationTargetException {
        //todo 多层list还没测
        if ("".equals(nextPath)) {
            return statementList;
        }

        //支持List<... Resp>结构的source解析
        if (source instanceof List) {
            List sources = (List) source;
            for (int i = 0; i < sources.size(); i++) {
                Object o = sources.get(i);
                buildStatementList(o, statementList, nextPath, i, transferSuffixIndex, finalPath, Mode, ignoreList, node);
            }
            return statementList;
        }

        String[] cutOutPath = cutOutPath(nextPath);
        String curPath = cutOutPath[0];
        nextPath = cutOutPath[1];

        finalPath = transferSuffixIndex == -1 ? finalPath + "." + curPath : finalPath + "[" + transferSuffixIndex + "]" + "." + curPath;
        finalPath = finalPath.charAt(0) == '.' ? finalPath.substring(1) : finalPath;
        finalPath = transferPrefixIndex == -1 ? finalPath : "[" + transferPrefixIndex + "]" + "." + finalPath;

        Object propertyValue;
        try {
            //todo 用检测属性的方法提高性能
            propertyValue = PropertyUtils.getProperty(source, curPath);
        } catch (NoSuchMethodException e) {
            //注:Bean拷贝工具在source和tar下存在相同属性名且类型为List但其指定的泛型不同时,拷贝后List的的实际泛型会变成source的泛型(即该情况下仅对比了属性名而不会考虑实际泛型不同)
            //issue 1.理论上存在该属性访问路径(parsingClass解析),实际上访问不到(list中的class变了) 2.理论无-实际有的情况无需考虑
            //Bean拷贝时需避免该情况
            //写模式加入到路径中()
//            if (Mode.equals("write")) {
//                statementList.add(finalPath);
//                //嵌套属性最外层为null时里层属性直接忽略,防止重复报错
//                ignoreList.add(finalPath);
//                return statementList;
//            }
            log.error("数据聚合-聚合对象属性实际访问路径解析异常,无法获取当前属性的读方法(确无该属性或Bean拷贝后属性发生变化),聚合对象={},属性路径={}", source.getClass().getName(), curPath);
            return statementList;
        } catch (NestedNullException e) {
            //多重嵌套属性访问里层属性时外层属性为空
            log.error("数据聚合-聚合对象属性实际访问路径解析异常,多重嵌套属性访问里层属性时外层属性为空,聚合对象={},属性路径={}", source.getClass().getName(), curPath);
            return statementList;
        }
        if (propertyValue == null) {
            statementList.add(finalPath);
            ignoreList.add(finalPath);

            return statementList;
        } else {
            //维护node
            Node curNode = node;
            while (curNode != null) {
                //todo 根节点的处理,write模式的处理
                if (curNode.curLevelPropertyName.equals(curPath) && curNode.nodeBindValMap.isEmpty()) {
                    createNodeLevelData(propertyValue, curNode);
                    break;
                } else {
                    curNode = curNode.prev;
                }
            }
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
                        transferPrefixIndex = -1;
                        buildStatementList(o, statementList, nextPath, transferPrefixIndex, i, finalPath, Mode, ignoreList, node);
                    } else {
                        if (!statementList.contains(finalPath)) {
                            statementList.add(finalPath);
                        }
                    }
                }
            }
        } else {
            if (isParsingClass(propertyValue.getClass()) && !nextPath.equals("")) {
                buildStatementList(propertyValue, statementList, nextPath, transferPrefixIndex, transferSuffixIndex, finalPath, Mode, ignoreList, node);
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
     *
     * @param absolutePathName
     * @param clazz
     * @param targetNode
     * @return
     * @throws ClassNotFoundException
     * @author zhengxin
     */
    //todo 按照执行器,聚合对象,以及分别对应的注解build模式重写?
    private AggregateTargetNode parsingClass(StringBuffer absolutePathName, Class<?> clazz, AggregateTargetNode targetNode, AggregateSourceNode sourceNode, String type, Map<Integer, Integer> levelMap) throws ClassNotFoundException {
        //todo 如果含有递归对象会有问题
        int hash = getHash(absolutePathName.toString(), clazz);
        if (!levelMap.containsKey(hash)) {
            levelMap.put(hash, 0);
        }
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
                    parsingClass(new StringBuffer(), source, targetNode, sourceNodeNow, "actuator", levelMap);
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

        int level = levelMap.get(hash);
        for (Field field : clazz.getDeclaredFields()) {
            if (isIgnoreField(field)) {//todo 递归对象的解析
                continue;
            }

            Class<?> propertyTypeClass = field.getType();
            String nextPathName = absolutePathName.toString() + "." + field.getName();
            nextPathName = nextPathName.charAt(0) == '.' ? nextPathName.substring(1) : nextPathName;

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
                    if (bindName == null || "".equals(bindName)) {

                    }
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
                        //只要有一个属性指定就可以
                        AtomicInteger bindTime = new AtomicInteger(0);
                        for (AggregateSourceNode aggregateSourceNode : targetNode.propertyAggregateMap.get(targetPropertyName)) {
                            if (!aggregateSourceNode.isSingleton()) {
                                for (Map.Entry<String, PropertyDescriptor> aggregateSourceNodeEntry : aggregateSourceNode.propertyAggregateMap.entrySet()) {
                                    if (aggregateSourceNodeEntry.getKey().equals(bindName)) {
                                        bindTime.getAndIncrement();
                                        aggregateSourceNode.setSingleton(true);
                                        if (bindTime.get() > 1) {
                                            log.error("数据聚合-聚合对象绑定执行器属性时执行器属性重复,聚合对象={},属性={}", targetNode.sourceClass.getName(), bindName);
                                            throw new BusinessException(120_000, "数据聚合-解析聚合对象绑定注解异常,执行器属性重复");
                                        }
                                    }
                                }
                            }
                        }
                        //设置执行器对应关系为多对一
                        //支持执行器对应关系随聚合对象变化
                        sourceNode.setSingleton(true);
                    }

                    aggregateTargetBindProperty = new AggregateTargetBindProperty(bindSourcePropertyName, nextPathName, propertyBind.required(), propertyBind.type(), 0, level);
                }
                if (field.isAnnotationPresent(DataAggregatePropertyMapping.class)) {
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

                    if (value == null || "".equals(value)) {

                    }
                    //todo 1.字段-在聚合对象中查找当前层级的同名字段 2.属性-查找当前层级同名属性 3.剩余字段如何处理?
                    bindSourcePropertyName = value;
                    aggregateTargetBindProperty = new AggregateTargetBindProperty(value, nextPathName, true, DataAggregatePropertyBind.BindType.DEFAULT, 1, level);
                }

                //约定 聚合对象下相同执行器可出现多次(需要指定别名)
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

            if (propertyTypeClass.isAssignableFrom(List.class)) {
                //todo List多重泛型嵌套测试
                ParameterizedType pType = (ParameterizedType) field.getGenericType();
                if (pType.getActualTypeArguments().length > 0) {
                    propertyTypeClass = Class.forName(pType.getActualTypeArguments()[0].getTypeName());
                }
            }
            if (isParsingClass(propertyTypeClass)) {
                StringBuffer curAbsolutePathName = new StringBuffer(absolutePathName.toString() + "." + field.getName());
                int parsingHash = getHash(curAbsolutePathName.toString(), propertyTypeClass);
                levelMap.put(parsingHash, level + 1);

                parsingClass(curAbsolutePathName, propertyTypeClass, targetNode, sourceNode, type, levelMap);
            }
        }

        return targetNode;
    }

    private List<AbstractDataAggregate> buildDataAggregate(Object sourceData, AggregatePrepare aggregatePrepare) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Node firstNode = aggregatePrepare.getDescNode();
        List<AbstractDataAggregate> instances = new ArrayList<>();
        if (firstNode == null) {
            return instances;
        }
        AggregateSourceNode aggregateSourceNode = aggregatePrepare.aggregateSourceNode;

        Node lastNode = aggregatePrepare.getLastNode();
        Map<String, AbstractDataAggregate> preValMap = new HashMap<>();
        //填充根节点绑定值
        createNodeLevelData(sourceData, firstNode);
        if (!aggregateSourceNode.isSingleton()) {
            //根据层级节点计算
            //从属性理论路径构建实际访问路径
            //todo 最里层的lineId数量期望为4,当前只有2  当数量为4时上层与下层之间如何对应? 当前node层级关系是平铺的,缺失了与上一级的被包含关系
            //todo buildStatementList里存在对应关系,node下级bindMap直接用buildStatementList的关系作为key?
            List<String> buildStatementList = buildStatementList(sourceData, new ArrayList(), lastNode.commonPrepareNodes.get(0).targetPropertyPath, -1, -1, "", "read", new ArrayList<>(), lastNode);
            for (int i = 0; i < buildStatementList.size(); i++) {
                String buildStatement = buildStatementList.get(i);
                if (instances.size() < buildStatementList.size()) {
                    instances.add(getOrderDataAggregateInstance(aggregateSourceNode));
                }

                //展开节点注入各级依赖值
                AbstractDataAggregate dataAggregate = instances.get(i);
                Map<Method, Object>[] maps = filterTarStatementList(buildStatement, aggregatePrepare.nodeMap);
                for (Map<Method, Object> methodObjectMap : maps) {
                    for (Map.Entry<Method, Object> methodObjectEntry : methodObjectMap.entrySet()) {
                        methodObjectEntry.getKey().invoke(dataAggregate, methodObjectEntry.getValue());
                    }
                }
            }
        } else {
            //显式指定为单例
        }

        return instances;
    }

    private List<AbstractDataAggregate> buildDataAggregate(Object sourceData, AggregatePrepare aggregatePrepare, Node nextNode, boolean isSingleton, List<AbstractDataAggregate> instances) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (nextNode == null) {
            return instances;
        }

        AggregateSourceNode aggregateSourceNode = aggregatePrepare.aggregateSourceNode;
        if (aggregatePrepare.getDescNode() == nextNode) {
            instances.add(getOrderDataAggregateInstance(aggregateSourceNode));
            for (AggregateBaseNode commonPrepareNode : nextNode.commonPrepareNodes) {
                String targetPropertyPath = commonPrepareNode.targetPropertyPath;
                //从属性理论路径构建实际访问路径
                List<String> buildStatementList = buildStatementList(sourceData, new ArrayList(), targetPropertyPath, -1, -1, "", "read", new ArrayList<>(), null);
                if (buildStatementList.size() > 0) {
                    if (buildStatementList.size() > 1) {
                        throw new BusinessException(120_000, "数据聚合-构建实际访问路径结果异常");
                    }

                    //注入依赖值
                    Object propertyVal = setActuatorProperty(sourceData, commonPrepareNode.method, commonPrepareNode, buildStatementList.get(0), instances.get(0));
                    if (propertyVal != null) {
                        aggregatePrepare.getDescNode().addBindVal(commonPrepareNode.method, propertyVal, 0);
                    }
                }
            }
        } else {
            //确定执行器绑定关系
            if (!isSingleton) {
                //根据层级节点计算
                for (AggregateBaseNode commonPrepareNode : nextNode.commonPrepareNodes) {
                    String targetPropertyPath = commonPrepareNode.targetPropertyPath;
                    List<String> buildStatementList = buildStatementList(sourceData, new ArrayList(), targetPropertyPath, -1, -1, "", "read", new ArrayList<>(), null);

                    for (int i = 0; i < buildStatementList.size(); i++) {
                        String buildStatement = buildStatementList.get(i);
                        AbstractDataAggregate instance = getOrderDataAggregateInstance(aggregateSourceNode);
                        Object propertyVal = setActuatorProperty(sourceData, commonPrepareNode.method, commonPrepareNode, buildStatementList.get(0), instances.get(0));

                        if (propertyVal != null) {
                            nextNode.addBindVal(commonPrepareNode.method, propertyVal, i);
                        }
                    }
                }
            } else {
                //显式指定为单例
                instances.add(getOrderDataAggregateInstance(aggregateSourceNode));
            }
        }

        List<Node> nextNodes = nextNode.next;
        if (nextNodes.size() > 1) {
            //issue:执行器属性绑定跨多个List时,最终数应为它们的直积,结合实际应用场景与此时数据反写判断与性能问题,暂不支持该情况
            log.error("数据聚合-执行器数据绑定解析异常:属性跨List绑定,聚合对象={},执行器={},绑定属性={}", sourceData.getClass(), aggregateSourceNode.sourceClass.getName(), nextNodes.toArray());
            throw new BusinessException(120_000, "数据聚合-执行器数据绑定解析异常:未支持属性跨List绑定");
        }

        return buildDataAggregate(sourceData, aggregatePrepare, nextNodes.get(0), isSingleton, instances);
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
     * 从实际可访问路径中匹配目标节点
     *
     * @param actualTarStatement
     * @author zhengxin
     */
    private Map<Method, Object>[] filterTarStatementList(String actualTarStatement, Map<String, Node> nodeMap) {
        //todo 省略处理字符串,放到buildstatement中?
        Pattern p = Pattern.compile("(\\[[^\\]]*\\])");
        String[] statementSplit = actualTarStatement.split("\\.");
        Map<Method, Object>[] maps = new HashMap[statementSplit.length];
        for (int i = 0; i < statementSplit.length; i++) {
            String str = statementSplit[i];
            Matcher m = p.matcher(str);
            if (m.find()) {
                String indexStr = m.group().substring(1, m.group().length() - 1);
                String nodeName = str.substring(0, str.lastIndexOf("["));
                Node node = nodeMap.get(nodeName);
                Map<Method, Object> methodObjectMap = node.nodeBindValMap.get(Integer.valueOf(indexStr));
                maps[i] = methodObjectMap;
            }
        }

        maps[maps.length - 1] = nodeMap.get("~").nodeBindValMap.get(0);
        return maps;
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

    private AbstractDataAggregate getOrderDataAggregateInstance(AggregateSourceNode sourceNode) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        AbstractDataAggregate orderDataAggregate = (AbstractDataAggregate) sourceNode.sourceClass.getDeclaredConstructor().newInstance();
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

    private int getHash(String pathName, Class<?> clazz) {
        //存在冲突可能,当前没有应对措施
        Field[] fields = clazz.getDeclaredFields();
        List<String> list = new ArrayList(List.of(pathName, clazz.getName(), "zx"));
        list.addAll(Arrays.stream(fields).map(Field::getName).collect(Collectors.toList()));
        return Objects.hash(list.toArray(new String[fields.length + 2]));
    }

    private void createNodeLevelData(Object source, Node node) {
        List<Object> sources = source instanceof List ? (List<Object>) source : List.of(source);
        for (AggregateBaseNode prepareNode : node.commonPrepareNodes) {
            for (int i = 0; i < sources.size(); i++) {
                Object prepareVal = null;
                try {
                    String[] tarPaths = prepareNode.targetPropertyPath.split("\\.");
                    prepareVal = PropertyUtils.getProperty(sources.get(i), tarPaths[tarPaths.length - 1]);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                node.addBindVal(prepareNode.method, prepareVal, i);
            }
        }
    }

    /**
     * 聚合对象
     */
    static class AggregateTargetNode {
        private final Class<?> sourceClass;
        //Class中所有属性平铺路径List(理论上可访问的属性)
        final List<String> propertyList = new ArrayList<>();

        //需要执行执行器map
        //key 聚合对象的相对属性名 val 对应执行器节点解析信息
        //todo 弱引用map
        final Map<String, List<AggregateSourceNode>> propertyAggregateMap = new HashMap<>();

        //key 执行器类名(或default) val <key 绑定/映射的执行器中的相对属性名 val 聚合对象相对属性名>
        final Map<String, Map<String, List<AggregateTargetBindProperty>>> bindPropertyMap = new HashMap<>();

        //todo key-聚合对象属性, val-执行器及所有属性绑定分布(List),保持层级关系(下一级扩容时复制上一级的属性),do中直接遍历执行即可
        //key 聚合对象属性 val 所有执行器中属性在当前聚合对象属性绑定关系的描述(维护了层级关系)
        final Map<String, List<AggregatePrepare>> aggregatePrepareMap = new HashMap<>();

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

        public AggregateTargetNode(Class<?> sourceClass) {
            this.sourceClass = sourceClass;
        }

        public void initAggregateNode() {
            for (Map.Entry<String, List<AggregateSourceNode>> targetPropertyEntry : propertyAggregateMap.entrySet()) {
                String curTargetPropertyName = targetPropertyEntry.getKey();
                //遍历属性绑定的所有执行器
                for (AggregateSourceNode sourceNode : targetPropertyEntry.getValue()) {
                    Map<String, List<AggregateTargetBindProperty>> classMap = bindPropertyMap.get(sourceNode.sourceClass.getName());
                    Map<String, List<AggregateTargetBindProperty>> defaultClassMap = bindPropertyMap.get("DEFAULT_CLASS_NAME");

                    AggregatePrepare aggregatePrepare = null;
                    if (!aggregatePrepareMap.containsKey(curTargetPropertyName)) {
                        aggregatePrepare = new AggregatePrepare(sourceNode);
                        aggregatePrepareMap.put(curTargetPropertyName, new ArrayList<>(List.of(aggregatePrepare)));
                    } else {
                        List<AggregatePrepare> aggregatePrepares = aggregatePrepareMap.get(curTargetPropertyName);
                        for (AggregatePrepare prepare : aggregatePrepares) {
                            if (prepare.aggregateSourceNode.sourceClass.equals(sourceNode.sourceClass)) {
                                aggregatePrepare = prepare;
                                continue;
                            }
                        }
                        if (aggregatePrepare == null) {
                            aggregatePrepare = new AggregatePrepare(sourceNode);
                            aggregatePrepares.add(aggregatePrepare);
                        }
                    }

                    //遍历执行器属性,如果属性在聚合对象中存在绑定关系,则建立描述节点
                    for (Map.Entry<String, PropertyDescriptor> entry : sourceNode.propertyAggregateMap.entrySet()) {
                        String sourcePropertyName = entry.getKey();

                        if (sourceNode.resourcePropertyMap.containsKey(sourcePropertyName) || sourceNode.allowPropertyList.contains(sourcePropertyName)) {
                            //该属性需要从spring中获取或为待反写的值
                            continue;
                        }

                        AggregateTargetBindProperty tarProperty;
                        tarProperty = getTarBindProperty(classMap, sourcePropertyName, 0);
                        tarProperty = tarProperty == null ? getTarBindProperty(defaultClassMap, sourcePropertyName, 0) : tarProperty;
                        if (tarProperty == null) {
                            //该执行器属性未指定绑定值(可能为无需绑定的类变量)
                            //为减少不必要注解当前执行器里无法区分类变量与期望绑定变量
                            continue;
                        }

                        Method writeMethod = entry.getValue().getWriteMethod();
                        String aggregateTargetPropertyName = tarProperty.getAggregateTargetPropertyName();
                        if (tarProperty.nodeType == 0) {
                            //绑定类型
                            int level = tarProperty.level;
                            if (level == 0) {
                                //普通属性
                                if (aggregatePrepare.getDescNode() == null) {
                                    aggregatePrepare.descNode = new Node("~", level);
                                }
                                aggregatePrepare.addCommonPrepareNode(new AggregateBaseNode(writeMethod, aggregateTargetPropertyName, tarProperty.isRequired()));
                            } else {
                                //list类型嵌套属性
                                Node firstNode = aggregatePrepare.getDescNode();
                                String[] nodePath = aggregateTargetPropertyName.split("\\.");
                                if (firstNode == null) {
                                    //初始化链路节点(可能从任意节点开始)
                                    List<Node> nodes = new ArrayList<>(List.of(new Node("~", 0)));
                                    int index = 0;
                                    while (index < level) {
                                        Node curNode = new Node(nodePath[index], index + 1);//todo 位移
                                        nodes.add(curNode);
                                        index++;
                                    }
                                    for (int i = 0; i < nodes.size(); i++) {
                                        Node curNode = nodes.get(i);
                                        if (i == 0) {
                                            firstNode = curNode;
                                            aggregatePrepare.descNode = firstNode;
                                        }
                                        curNode.prev = i == 0 ? null : nodes.get(i - 1);
                                        if (i + 1 < nodes.size()) {
                                            curNode.next.add(nodes.get(i + 1));
                                        }
                                    }
                                }

                                //遍历链路获取当前Level对应的最终节点
                                Node tarNode = firstNode;
                                for (int i = 0; i < level; i++) {
                                    Node curNode = null;
                                    for (Node node : tarNode.next) {
                                        if (nodePath[i].equals(node.curLevelPropertyName)) {
                                            curNode = node;
                                            continue;
                                        }
                                    }

                                    if (curNode != null) {
                                        tarNode = curNode;
                                    } else {
                                        //节点不存在则新建
                                        Node newNode = new Node(nodePath[i], i + 1);
                                        newNode.prev = tarNode;
                                        tarNode.next.add(newNode);
                                        tarNode = newNode;
                                    }
                                }
                                tarNode.commonPrepareNodes.add(new AggregateBaseNode(writeMethod, aggregateTargetPropertyName, tarProperty.isRequired()));
                            }
                        }
                    }
                    aggregatePrepare.iniAggregatePrepare(sourceClass);
                }
            }
        }
    }

    static class AggregateSourceNode implements Cloneable {
        /* 标记执行器是否单例 */
        boolean singleton = false;
        final Class<?> sourceClass;
        //待注入值的属性
        final List<String> ignorePropertyList = new ArrayList<>();
        //待反写的属性
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
        //执行器相对属性名
        private final String actuatorPropertyName;
        //聚合对象相对属性名
        private final String aggregateTargetPropertyName;
        //标示绑定到执行器的属性是否必要
        private final boolean required;
        //list类型嵌套属性的层级(非List下的普通属性为0)
        private int level;

        public AggregateTargetBindProperty(String v1, String v2, boolean v3, DataAggregatePropertyBind.BindType v4, int v5, int level) {
            this.actuatorPropertyName = v1;
            this.aggregateTargetPropertyName = v2;
            this.required = v3;
            this.bindType = v4;
            this.nodeType = v5;
            this.level = level;
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

    /**
     * 执行器属性被绑定分布
     */
    static class AggregatePrepare {
        private final AggregateSourceNode aggregateSourceNode;

        //执行器属性绑定描述节点
        private Node descNode;
        private Node lastNode;

        private Map<String, Node> nodeMap = new HashMap<>();

        public AggregatePrepare(AggregateSourceNode var) {
            this.aggregateSourceNode = var;
        }

        public void iniAggregatePrepare(Class<?> sourceClass) {
            int size;
            Node topNode;
            List<Node> nextNodes = List.of(descNode);
            while ((size = nextNodes.size()) > 0) {
                if (size == 1) {
                    topNode = nextNodes.get(0);
                    if (!nodeMap.containsKey(topNode.curLevelPropertyName)) {
                        nodeMap.put(topNode.curLevelPropertyName, topNode);
                    }
                    if (topNode.next.size() > 0) {
                        nextNodes = nextNodes.get(0).next;
                    } else {
                        break;
                    }
                } else {
                    //issue:执行器属性绑定跨多个List时,最终数应为它们的直积,结合实际应用场景与此时数据反写判断与性能问题,暂不支持该情况
                    log.error("数据聚合-执行器数据绑定解析异常:属性跨List绑定,聚合对象={},执行器={},绑定属性={}", sourceClass.getName(), aggregateSourceNode.sourceClass.getName(), nextNodes.toArray());
                    throw new BusinessException(120_000, "数据聚合-执行器数据绑定解析异常:未支持属性跨List绑定");
                }
            }
            this.lastNode = nextNodes.get(0);
        }

        public void addCommonPrepareNode(AggregateBaseNode node) {
            this.descNode.commonPrepareNodes.add(node);
        }

        public Node getDescNode() {
            return descNode;
        }

        public Node getLastNode() {
            return lastNode;
        }
    }

    static class AggregateBaseNode {
        //执行器属性读写方法
        private final Method method;

        //聚合对象属性路径
        private final String targetPropertyPath;

        //标示绑定到执行器的属性是否必要
        private final boolean required;

        public AggregateBaseNode(Method method, String targetPropertyPath, boolean required) {
            this.method = method;
            this.required = required;
            this.targetPropertyPath = targetPropertyPath;
        }

        public boolean isRequired() {
            return required;
        }
    }

    private static class Node {
        Node prev;
        //list属性绑定列表(可能存在多个),不存在List类型时为null,标记作用(每个List中存在任意属性)
        List<Node> next = new ArrayList<>();
        //普通属性绑定列表
        private List<AggregateBaseNode> commonPrepareNodes = new ArrayList<>();

        private List<Map<Method, Object>> nodeBindValMap = new ArrayList<>();

        private String id;
        private int level;
        //当前节点在聚合对象中的属性名
        private String curLevelPropertyName;

        Node(String curLevelPropertyName, int level) {
            this.level = level;
            this.id = curLevelPropertyName + "-" + level;
            this.curLevelPropertyName = curLevelPropertyName;
        }

        public boolean addBindVal(Method method, Object val, int index) {
            if (nodeBindValMap.size() - 1 >= index) {
                nodeBindValMap.get(index).put(method, val);
            } else {
                Map<Method, Object> map = new HashMap<>();
                map.put(method, val);
                nodeBindValMap.add(map);
            }
            return true;
        }
    }

}
