package com.vd.canary.obmp.aggregate;

import cn.hutool.json.JSONUtil;
import com.vd.canary.core.bo.ResponseBO;
import com.vd.canary.core.bo.ResponsePageBO;
import com.vd.canary.core.exception.BusinessException;
import com.vd.canary.obmp.aggregate.annotation.DataAggregatePropertyBind;
import com.vd.canary.obmp.aggregate.annotation.DataAggregatePropertyMapping;
import com.vd.canary.obmp.aggregate.annotation.DataAggregateType;
import com.vd.canary.obmp.aggregate.annotation.TypeProfile;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    //存执行器蓝本,get方法返回其clone
    private ConcurrentHashMap<String, AggregateSourceNode> AggregateSourceMap = new ConcurrentHashMap<>();

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

    static Map<Class, Object[]> basicTypeMap = new HashMap() {{
        put(int.class, new Object[]{Integer.class, 0});
        put(byte.class, new Object[]{Byte.class, 0xFFFFFFFF});
        put(short.class, new Object[]{Short.class, 0xFFFFFFFF});
        put(long.class, new Object[]{Long.class, 0});
        put(float.class, new Object[]{Float.class, 0});
        put(double.class, new Object[]{Double.class, 0});
        put(boolean.class, new Object[]{Boolean.class, false});
        put(char.class, new Object[]{Character.class, '\u0000'});
        put(Integer.class, new Object[]{int.class, 0});
        put(Byte.class, new Object[]{byte.class, 0xFFFFFFFF});
        put(Short.class, new Object[]{Short.class, 0xFFFFFFFF});
        put(Long.class, new Object[]{Long.class, 0});
        put(Float.class, new Object[]{float.class, 0});
        put(Double.class, new Object[]{double.class, 0});
        put(Boolean.class, new Object[]{boolean.class, false});
        put(Character.class, new Object[]{char.class, '\u0000'});
    }};

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
    //todo BeanUtils性能:与反射存在2个数量级差距(考虑替换为完全自行反射) https://www.xiaobo.li/?p=1457 https://www.cnblogs.com/Frank-Hao/p/5839140.html
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
        boolean isList;
        if (propertyValue == null) {
            statementList.add(finalPath);
            ignoreList.add(finalPath);

            return statementList;
        } else {
            isList = propertyValue instanceof List;
            //维护node
            //todo 以数据结构代替字符串操作(即将展示路径如xx.[].xx用数据结构代替)
            String[] var = finalPath.split("\\.");
            if (!var[var.length - 1].contains("[")) {
                Node curNode = node;
                while (curNode != null) {
                    //todo 根节点的处理,write模式的处理
                    if (curNode.curLevelPropertyName.equals(curPath)) {
                        createNodeLevelData(propertyValue, curNode, finalPath, isList);
                        break;
                    } else {
                        curNode = curNode.prev;
                    }
                }
            }
        }

        if (isList) {
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
    private AggregateTargetNode parsingClass(StringBuffer absolutePathName, Class<?> clazz, AggregateTargetNode targetNode, AggregateSourceNode sourceNode, String type, Map<Integer, Integer> levelMap) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        //todo 如果含有递归对象会有问题
        int hash = getHash(absolutePathName.toString(), clazz);
        if (!levelMap.containsKey(hash)) {
            levelMap.put(hash, 0);
        }
        String targetPropertyName = absolutePathName.toString().equals("") ? "~" : absolutePathName.toString();
        targetPropertyName = targetPropertyName.charAt(0) == '.' ? targetPropertyName.substring(1) : targetPropertyName;

        if (clazz.isAnnotationPresent(DataAggregateType.class)) {
            Class[] sourceClass;
            String[] classNames;
            List<AggregateSourceNode> aggregateSourceNodes = new ArrayList<>();
            TypeProfile[] profiles = clazz.getAnnotation(DataAggregateType.class).profile();
            if (profiles.length > 0) {
                AggregateSourceNode aggregateSourceNode;
                for (TypeProfile profile : profiles) {
                    String className;
                    Class value = profile.value();
                    if (value != Object.class) {
                        aggregateSourceNode = getOrCreateSourceNode(value, value.getName(), targetNode, levelMap);
                    } else if (!(className = profile.className()).equals("")) {
                        aggregateSourceNode = getOrCreateSourceNode(null, className, targetNode, levelMap);
                    } else {
                        log.error("数据聚合-解析TypeProfile异常-无效的执行器,聚合对象={},执行器className={}", targetNode.sourceClass.getName(), targetPropertyName);
                        throw new BusinessException(120_000, "数据聚合-解析TypeProfile异常-无效的执行器");
                    }

                    if (profile.mode().equals(TypeProfile.Mode.MANY_TO_ONE)) {
                        //设置执行器对应关系为多对一
                        //支持执行器对应关系随聚合对象变化
                        aggregateSourceNode.setSingleton(true);
                    }

                    aggregateSourceNodes.add(aggregateSourceNode);
                }
            } else if ((sourceClass = clazz.getAnnotation(DataAggregateType.class).value()).length > 0) {
                for (Class aClass : sourceClass) {
                    aggregateSourceNodes.add(getOrCreateSourceNode(aClass, aClass.getName(), targetNode, levelMap));
                }
            } else if ((classNames = clazz.getAnnotation(DataAggregateType.class).classNames()).length > 0) {
                for (String className : classNames) {
                    aggregateSourceNodes.add(getOrCreateSourceNode(null, className, targetNode, levelMap));
                }
            } else {
                log.error("数据聚合-解析DataAggregateType异常-无效的执行器,聚合对象={},执行器className={}", targetNode.sourceClass.getName(), targetPropertyName);
                throw new BusinessException(120_000, "数据聚合-解析DataAggregateType异常-无效的执行器");
            }

            for (AggregateSourceNode aggregateSourceNode : aggregateSourceNodes) {
                //设置解析对象待执行器
                if (!targetNode.propertyAggregateMap.containsKey(targetPropertyName)) {
                    targetNode.propertyAggregateMap.put(targetPropertyName, Arrays.asList(aggregateSourceNode));
                } else {
                    targetNode.propertyAggregateMap.get(targetPropertyName).add(aggregateSourceNode);
                }
            }
        }

        int level = levelMap.get(hash);
        for (Field field : clazz.getDeclaredFields()) {
            if (isIgnoreField(field)) {//todo 递归对象的解析
                continue;
            }

            Class<?> propertyTypeClass = field.getType();
            String nextPathName;
            String tmpAbsolutePathName = absolutePathName.toString();
            if (tmpAbsolutePathName.equals("")) {
                nextPathName = field.getName();
            } else {
                tmpAbsolutePathName = tmpAbsolutePathName.charAt(0) == '.' ? tmpAbsolutePathName.substring(1) : tmpAbsolutePathName;
                nextPathName = tmpAbsolutePathName + "." + field.getName();
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
                PropertyDescriptor propertyDescriptor = findTar(clazz, field.getName());//todo 不需要每次重新获取类,设置缓存
                AggregateBaseNode baseNode = new AggregateBaseNode(propertyDescriptor.getWriteMethod(), propertyDescriptor.getReadMethod(), field, nextPathName);
                sourceNode.propertyAggregateMap.put(nextPathName, baseNode);
                baseNode.initAggregateBaseNodeActuator(sourceNode.propertyAggregateMap.get(tmpAbsolutePathName));
            } else {
                //解析聚合对象属性绑定注解
                targetNode.propertyList.add(nextPathName);

                List<AggregateTargetBindProperty> aggregateTargetBindProperties = new ArrayList<>();
                if (field.isAnnotationPresent(DataAggregatePropertyBind.class)) {
                    DataAggregatePropertyBind propertyBind = field.getAnnotation(DataAggregatePropertyBind.class);
                    String bindName = propertyBind.value();
                    if (bindName == null || "".equals(bindName)) {

                    }
                    Class bindClass;
                    String bindClassStr;
                    String bindSourceClassName = null;
                    if ((bindClass = propertyBind.className()) != Object.class) {
                        bindSourceClassName = bindClass.getName();
                    } else if (!(bindClassStr = propertyBind.classNameStr()).equals("")) {
                        bindSourceClassName = bindClassStr;
                    }

                    aggregateTargetBindProperties.add(new AggregateTargetBindProperty(bindName, nextPathName, propertyBind.required(), 0, bindSourceClassName, propertyBind.primary(), level));
                }
                if (field.isAnnotationPresent(DataAggregatePropertyMapping.class)) {
                    DataAggregatePropertyMapping propertyMapping = field.getAnnotation(DataAggregatePropertyMapping.class);
                    String value = propertyMapping.value();
                    String classNameStr = propertyMapping.classNameStr();
                    Class<?> mappingClass = propertyMapping.className();

                    String bindSourceClassName = null;
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
                    aggregateTargetBindProperties.add(new AggregateTargetBindProperty(value, nextPathName, true, 1, bindSourceClassName, "", level));
                }

                //约定 聚合对象下相同执行器可出现多次(需要指定别名)
                for (AggregateTargetBindProperty aggregateTargetBindProperty : aggregateTargetBindProperties) {
                    String bindSourcePropertyName = aggregateTargetBindProperty.actuatorPropertyName;
                    if (bindSourcePropertyName.equals("")) {
                        continue;
                    }
                    String bindSourceClassName = aggregateTargetBindProperty.actuatorClassName;
                    if (targetNode.bindPropertyMap.containsKey(bindSourceClassName)) {
                        Map<String, List<AggregateTargetBindProperty>> listMap = targetNode.bindPropertyMap.get(bindSourceClassName);
                        if (targetNode.getTarBindProperty(listMap, new ArrayList<>(List.of(bindSourcePropertyName)), 0).size() > 1) {
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

                PropertyDescriptor propertyDescriptor = findTar(clazz, field.getName());//todo 不需要每次重新获取类,设置缓存,只有bind和mapping注解下的才需要
                AggregateBaseNode baseNode = new AggregateBaseNode(propertyDescriptor.getWriteMethod(), propertyDescriptor.getReadMethod(), field, nextPathName);
                targetNode.propertyBaseNodeMap.put(nextPathName, baseNode);
                baseNode.initAggregateBaseNodeTar(targetNode.propertyBaseNodeMap.get(tmpAbsolutePathName));
            }

            if (propertyTypeClass.isAssignableFrom(List.class)) {
                //todo List多重泛型嵌套测试
                ParameterizedType pType = (ParameterizedType) field.getGenericType();
                if (pType.getActualTypeArguments().length > 0) {
                    propertyTypeClass = (Class<?>) pType.getActualTypeArguments()[0];
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
        List<AbstractDataAggregate> instances = new ArrayList<>();
        Node firstNode = aggregatePrepare.getDescNode();
        if (firstNode == null) {
            return instances;
        }
        AbstractDataAggregate instance = null;
        //AggregateBaseNode belongContainer = null;
        AggregateSourceNode aggregateSourceNode = aggregatePrepare.aggregateSourceNode;
        if (aggregateSourceNode.isSingleton()) {
            instances.add(instance = aggregateSourceNode.getEmptyDataAggregate());
        }
        Node lastNode = aggregatePrepare.getLastNode();

        //填充根节点绑定值
        createNodeLevelData(sourceData, firstNode, "~", sourceData instanceof List);

        //根据层级节点计算
        //从属性理论路径构建实际访问路径
        List<String> buildStatementList = buildStatementList(sourceData, new ArrayList(), lastNode.commonPrepareNodes.get(0).targetPropertyPath, -1, -1, "", "read", new ArrayList<>(), lastNode);
        for (int i = 0; i < buildStatementList.size(); i++) {
            String buildStatement = buildStatementList.get(i);

            if (!aggregateSourceNode.isSingleton() && instances.size() < buildStatementList.size()) {
                instances.add(aggregateSourceNode.getEmptyDataAggregate());
                instance = instances.get(i);
            }

            //Map<Method, Object>[] maps = filterTarStatementList(buildStatement, aggregatePrepare.nodeMap);
            //展开节点注入各级依赖值
            Object containerTypeInstance = null;
            String str = "";
            String[] statementSplit = buildStatement.split("\\.");
            for (int j = 0; j < statementSplit.length; j++) {
                String sj = statementSplit[j];
                if (sj.charAt(0) == '[') {
                    //处理[].xxx的情况
                    str = sj;
                    continue;
                }

                Map<AggregateBaseNode, Object> methodObjectMap;
                int index = sj.lastIndexOf("[");
                if (index == -1) {
                    //处理根节点
                    methodObjectMap = aggregatePrepare.nodeMap.get("~").nodeBindValMap.get("~");
                } else {
                    str = j == 0 ? sj : str + "." + sj;
                    String nodeName = sj.substring(0, index);
                    Node node = aggregatePrepare.nodeMap.get(nodeName);
                    methodObjectMap = node.nodeBindValMap.get(str);
                }
                if (methodObjectMap == null) {
                    continue;
                }
                //处理每一级展开节点下的所有绑定值(xx[].all)
                for (Map.Entry<AggregateBaseNode, Object> methodObjectEntry : methodObjectMap.entrySet()) {
                    AggregateBaseNode baseNode = methodObjectEntry.getKey();
                    aggregateSourceNode.waitResetBaseNodes.add(baseNode);
                    Object value = methodObjectEntry.getValue();
                    if (!aggregateSourceNode.isSingleton()) {
                        //todo baseNode传入的是baseNode的上一级
                        baseNode.writeMethod.invoke(instance, value);
                    } else {
                        if (j == 0) {
                            //belongContainer = baseNode.belongContainer;
                            containerTypeInstance = baseNode.getNodeElementCloneable();
                        }
                        baseNode.injectNodeVal(value, containerTypeInstance);
                    }
                }
            }
        }
        for (AbstractDataAggregate abstractDataAggregate : instances) {
            //从节点回归属性到实例节点
            initDataAggregateInstance(aggregateSourceNode, abstractDataAggregate);
        }

//        Iterator<AggregateBaseNode> iterator = waitResetBaseNode.iterator();
//        while (iterator.hasNext()) {
//            iterator.next().clear();
//        }
        return instances;
    }

    // TODO: 这里反写mapping的值,或者把整个反写逻辑放过来?
    private void buildDataAggregate1(Object sourceData, Object targetData, AggregatePrepare aggregatePrepare, AggregateTargetNode targetNode) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Node firstNode = aggregatePrepare.mappingNode;
        if (firstNode == null) {
            return;
        }
        AbstractDataAggregate instance = null;
        //AggregateBaseNode belongContainer = null;
        AggregateSourceNode aggregateSourceNode = aggregatePrepare.aggregateSourceNode;

        Node lastNode = firstNode.next.get(0);//todo 获取最终级

        //填充根节点绑定值
        createNodeLevelData(sourceData, firstNode, "~", sourceData instanceof List);

        //根据层级节点计算
        //从属性理论路径构建实际访问路径(同时维护了节点此次的值)
        List<String> buildStatementList = buildStatementList(sourceData, new ArrayList(), lastNode.commonPrepareNodes.get(0).aggregatePropertyPath, -1, -1, "", "read", new ArrayList<>(), lastNode);

        //todo 多层级嵌套实现
        if (lastNode.commonPrepareNodes.get(0).isContainer()) {
            //获取关联key
            AggregateBaseNode primaryAggregateBaseNode = aggregateSourceNode.primaryAggregateBaseNode;
            //执行器节点数组
            ArrayList actuatorContainer = (ArrayList) lastNode.getContainer(aggregateSourceNode, sourceData);
            //聚合对象节点数组
            AggregateBaseNode aggregateBaseNode = targetNode.propertyBaseNodeMap.get(lastNode.commonPrepareNodes.get(0).targetPropertyPath);
            ArrayList tarContainer = (ArrayList) aggregateBaseNode.getContainer(aggregatePrepare.lastNode, targetData);

            //key->将关联键的值 val->bindValMap(属性层下的所有映射值)
            Map<Object,Map<AggregateBaseNode, Object>> actuatorKeyMap = new HashMap<>();
            for (int i = 0; i < actuatorContainer.size(); i++) {
                Object actuatorVal = actuatorContainer.get(i);
                Map<AggregateBaseNode, Object> nodeObjectMap = lastNode.nodeBindValMap.get(lastNode.curLevelPropertyName + "[" + i + "]");
                actuatorKeyMap.put(primaryAggregateBaseNode.readMethod.invoke(actuatorVal), nodeObjectMap);
            }

            AggregateBaseNode tarBaseNode = targetNode.propertyBaseNodeMap.get(primaryAggregateBaseNode.targetPropertyPath);
            for (Object tarVal : tarContainer) {
                Object tarKeyVal = tarBaseNode.readMethod.invoke(tarVal);
                Map<AggregateBaseNode, Object> nodeObjectMap = actuatorKeyMap.get(tarKeyVal);
                if (nodeObjectMap != null) {
                    for (Map.Entry<AggregateBaseNode, Object> nodeObjectEntry : nodeObjectMap.entrySet()) {
                        targetNode.propertyBaseNodeMap.get(nodeObjectEntry.getKey().targetPropertyPath).writeMethod.invoke(tarVal, nodeObjectEntry.getValue());
                    }
                }
            }
        }
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
//    private Map<Method, Object>[] filterTarStatementList(String actualTarStatement, Map<String, Node> nodeMap) {
//        //todo 省略处理字符串,放到buildstatement中?
//        Pattern p = Pattern.compile("(\\[[^\\]]*\\])");
//        String[] statementSplit = actualTarStatement.split("\\.");
//        Map<Method, Object>[] maps = new HashMap[statementSplit.length];
//        for (int i = 0; i < statementSplit.length; i++) {
//            String str = statementSplit[i];
//            Matcher m = p.matcher(str);
//            if (m.find()) {
//                String indexStr = m.group().substring(1, m.group().length() - 1);
//                String nodeName = str.substring(0, str.lastIndexOf("["));
//                Node node = nodeMap.get(nodeName);
//                Map<Method, Object> methodObjectMap = node.nodeBindValMap.get(Integer.valueOf(indexStr));
//                maps[i] = methodObjectMap;
//            }
//        }
//
//        maps[maps.length - 1] = nodeMap.get("~").nodeBindValMap.get(0);
//        return maps;
//    }

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
        //todo 重写
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

    private AbstractDataAggregate initDataAggregateInstance(AggregateSourceNode sourceNode, AbstractDataAggregate instance) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        //实例化执行器中需注入的嵌套属性
        for (String requirement : sourceNode.ignorePropertyList) {
            AggregateBaseNode baseNode = sourceNode.propertyAggregateMap.get(requirement);
            if (baseNode == null) {
                throw new BusinessException(120_000, "实例化执行器异常-无法获取获取需注入属性的解析节点");
            }
            Object propertyInstance = baseNode.propertyInstance;
            baseNode.writeMethod.invoke(instance, propertyInstance);
        }

        Map<Method, Object> initMap = new HashMap<>();
        for (Map.Entry<String, Object> classEntry : sourceNode.resourcePropertyMap.entrySet()) {
            initMap.put(sourceNode.propertyAggregateMap.get(classEntry.getKey()).writeMethod, classEntry.getValue());
        }
        instance.init(initMap);
        return instance;
    }

    private void setAggregateBindVal(AbstractDataAggregate instance, Method method, Object val) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<?> declaringClass = method.getDeclaringClass();
        while (instance.getClass() != declaringClass) {
            declaringClass.getDeclaredConstructor().newInstance();
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

    private int getHash(String pathName, Class<?> clazz) {
        //存在冲突可能,当前没有应对措施
        Field[] fields = clazz.getDeclaredFields();
        List<String> list = new ArrayList(List.of(pathName, clazz.getName(), "zx"));
        list.addAll(Arrays.stream(fields).map(Field::getName).collect(Collectors.toList()));
        return Objects.hash(list.toArray(new String[fields.length + 2]));
    }

    private void createNodeLevelData(Object source, Node node, String statement, boolean isList) {
        List<Object> sources = isList ? (List<Object>) source : List.of(source);
        for (AggregateBaseNode prepareNode : node.commonPrepareNodes) {
            for (int i = 0; i < sources.size(); i++) {
                try {
                    String[] tarPaths = node.nodeType == 0 ? prepareNode.targetPropertyPath.split("\\.") : prepareNode.aggregatePropertyPath.split("\\.");
                    String path = tarPaths[tarPaths.length - 1];
                    //todo 使用维护的resp的读写方法?
                    Object prepareVal = PropertyUtils.getProperty(sources.get(i), path);
                    String var = isList ? statement + "[" + i + "]" : statement;
                    node.addBindVal(prepareNode, prepareVal, var);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isReWrite(String curReWriteProperty, Map<String, Object> cacheMap) {
        for (String key : cacheMap.keySet()) {
            if (curReWriteProperty.startsWith(key)) {
                return true;
            }
        }
        return false;
    }

    private AggregateSourceNode getOrCreateSourceNode(Class source, String className, AggregateTargetNode targetNode, Map<Integer, Integer> levelMap) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        if (!AggregateSourceMap.containsKey(className)) {
            if (source == null) {
                //尝试加载执行器
                try {
                    source = Class.forName(className);
                } catch (ClassNotFoundException classNotFoundException) {
                    log.error("无法为聚合对象加载指定执行器-ClassNotFoundException,聚合对象={},执行器className={}", targetNode.sourceClass.getName(), className);
                    throw new RuntimeException("无法为聚合对象加载指定执行器");
                }
            }
            AggregateSourceNode sourceNodeNow = new AggregateSourceNode(source);
            parsingClass(new StringBuffer(), source, targetNode, sourceNodeNow, "actuator", levelMap);
            AggregateSourceMap.put(className, sourceNodeNow);
        }

        return AggregateSourceMap.get(className).clone();
    }

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
    //todo 关键性能节点多线程,静态解析放到启动
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
            Map<String, Map<String, List<AggregateTargetBindProperty>>> bindPropertyMap = targetNode.bindPropertyMap;
            for (AggregatePrepare aggregatePrepare : propertyPrepareEntity.getValue()) {
                //为执行器描述节点建立此次请求的层级数据
                List<AbstractDataAggregate> instances = buildDataAggregate(responseData, aggregatePrepare);
                if (instances.size() == 0) {
                    continue;
                }

                AggregateSourceNode sourceNode = aggregatePrepare.aggregateSourceNode;
                Map<String, List<AggregateTargetBindProperty>> classMap, defaultClassMap;
                classMap = bindPropertyMap.get(sourceNode.sourceClass.getName());
                defaultClassMap = bindPropertyMap.get("DEFAULT_CLASS_NAME");

                Map<String, Object> reWriteCacheMap = new HashMap<>();
                Map<String, List<String>> statementCacheMap = new HashMap<>(sourceNode.allowPropertyList.size());
                for (int i = 0; i < instances.size(); i++) {
                    AbstractDataAggregate dataAggregate = instances.get(i);
                    //执行聚合方法 todo 代理
                    if (!dataAggregate.isActuatorFlag()) {
                        continue;
                    }
                    dataAggregate.doDataAggregate();
                    //数据反写
                    //优先使用mapping注解
                    buildDataAggregate1(dataAggregate, responseData, aggregatePrepare, targetNode);

                    for (String waitWriteVal : sourceNode.allowPropertyList) {
                        //对于执行器任意自定义属性,其上层被反写后忽略所有下层
                        if (isReWrite(waitWriteVal, reWriteCacheMap)) {
                            continue;
                        }

                        List<String> targetStatementList;
                        if (!statementCacheMap.containsKey(waitWriteVal)) {
                            //在聚合对象中查找属性对应的访问路径
                            //@DataAggregateType注解所在层级优先
                            //~表示根路径
                            String possiblePath = curTargetPropertyName.equals("~") ? waitWriteVal : curTargetPropertyName + "." + waitWriteVal;
                            //todo 1.可以在read模式时一起返回write,做区分,这样只用调用一次 2.当对象为List时相同属性的实际路径只用解析一次(当前解析n次) 3.存在同名属性(但并不是期望的反写值)
                            targetStatementList = buildStatementList(responseData, new ArrayList(), possiblePath, -1, -1, "", "write", new ArrayList<>(), null);

                            if (targetStatementList.size() == 0) {
                                //todo 完整查找还要考虑数量匹配的问题（到上层可能找到了但是与执行器数量可能对不上，会导致不断覆盖）
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
                            reWriteCacheMap.put(waitWriteVal, null);
                            AggregateBaseNode aggregateBaseNode = sourceNode.propertyAggregateMap.get(waitWriteVal);
                            Object val = aggregateBaseNode.readMethod.invoke(dataAggregate);

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
                    reWriteCacheMap.clear();
                }
                sourceNode.clear();
            }
        }
    }

    public Object getClassInstance(Class<?> clazz) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
//        Class type = (Class) clazz.getField("TYPE").get(null);
//        if (type.isPrimitive()) {
//            return clazz.getDeclaredConstructor(type).newInstance();
//        }
        //todo 1.非包装类型是否需要初始化 2.map里直接放对应的初始值(不用反射执行,是否会造成引用问题)
        Object[] objects = basicTypeMap.get(clazz);
        if (clazz.isPrimitive()) {
            //基本类
            return ((Class) (objects[0])).getDeclaredConstructor(clazz).newInstance(objects[1]);
        } else if (basicTypeMap.containsKey(clazz)) {
            //包装类
            return clazz.getDeclaredConstructor(((Class) (objects[0]))).newInstance(objects[1]);
        } else {
            //todo 私有化的构造函数
            if (clazz.equals(BigDecimal.class)) {
                return BigDecimal.ZERO;
            }
            if (clazz.equals(LocalDateTime.class)) {
                return LocalDateTime.now();
            }
            return clazz.getDeclaredConstructor().newInstance();
        }
    }

    /**
     * 聚合对象
     */
    static class AggregateTargetNode {
        private final Class<?> sourceClass;
        //Class中所有属性平铺路径List(理论上可访问的属性)
        final List<String> propertyList = new ArrayList<>();

        final Map<String, AggregateBaseNode> propertyBaseNodeMap = new HashMap<>();

        //需要执行执行器map
        //key 聚合对象的相对属性名 val 对应执行器节点解析信息
        //todo 弱引用map
        final Map<String, List<AggregateSourceNode>> propertyAggregateMap = new HashMap<>();

        //key 执行器类名(或default) val <key 绑定/映射的执行器中的相对属性名 val 聚合对象相对属性名>
        final Map<String, Map<String, List<AggregateTargetBindProperty>>> bindPropertyMap = new HashMap<>();

        //todo key-聚合对象属性, val-执行器及所有属性绑定分布(List),保持层级关系(下一级扩容时复制上一级的属性),do中直接遍历执行即可
        //key 聚合对象属性 val 所有执行器中属性在当前聚合对象属性绑定关系的描述(维护了层级关系)
        final Map<String, List<AggregatePrepare>> aggregatePrepareMap = new HashMap<>();

        public List<AggregateTargetBindProperty> getTarBindProperty(Map<String, List<AggregateTargetBindProperty>> map, List<String> sourcePropertyNames, int type) {
            List<AggregateTargetBindProperty> aggregateTargetBindProperties = new ArrayList<>();

            if (map == null) {
                return aggregateTargetBindProperties;
            }
            Iterator<String> iterator = sourcePropertyNames.iterator();
            while (iterator.hasNext()) {
                String sourcePropertyName = iterator.next();
                if (map.containsKey(sourcePropertyName)) {
                    Optional<AggregateTargetBindProperty> targetProperty = map.get(sourcePropertyName).stream().filter(tarProperty -> {
                        if (tarProperty.nodeType == type) {
                            return true;
                        }
                        return false;
                    }).findFirst();
                    if (targetProperty.isPresent()) {
                        iterator.remove();
                        aggregateTargetBindProperties.add(targetProperty.get());
                    }
                }
            }
            return aggregateTargetBindProperties;
        }

        public List<AggregateTargetBindProperty> getTarBindProperty(Map<String, List<AggregateTargetBindProperty>> map, List<String> sourcePropertyNames) {
            List<AggregateTargetBindProperty> aggregateTargetBindProperties = new ArrayList<>();

            if (map == null) {
                return aggregateTargetBindProperties;
            }
            Iterator<String> iterator = sourcePropertyNames.iterator();
            while (iterator.hasNext()) {
                String sourcePropertyName = iterator.next();
                if (map.containsKey(sourcePropertyName)) {
                    List<AggregateTargetBindProperty> bindProperties = map.get(sourcePropertyName);
                    aggregateTargetBindProperties.addAll(bindProperties);
                }
            }
            return aggregateTargetBindProperties;
        }

        public AggregateTargetNode(Class<?> sourceClass) {
            this.sourceClass = sourceClass;
        }

        public void initAggregateNode() {
            for (Map.Entry<String, List<AggregateSourceNode>> targetPropertyEntry : propertyAggregateMap.entrySet()) {
                String curTargetPropertyName = targetPropertyEntry.getKey();
                //遍历属性绑定的所有执行器
                for (AggregateSourceNode sourceNode : targetPropertyEntry.getValue()) {
                    Map<String, List<AggregateTargetBindProperty>> classMap, defaultClassMap;
                    classMap = bindPropertyMap.get(sourceNode.sourceClass.getName());
                    defaultClassMap = bindPropertyMap.get("DEFAULT_CLASS_NAME");

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
                    //遍历执行器属性,如果属性在聚合对象中存在绑定/映射关系,则建立描述节点
                    for (Map.Entry<String, AggregateBaseNode> entry : sourceNode.propertyAggregateMap.entrySet()) {
                        String sourcePropertyName = entry.getKey();

                        if (sourceNode.resourcePropertyMap.containsKey(sourcePropertyName)) {
                            //该属性需要从spring中获取
                            continue;
                        }

                        AggregateBaseNode aggregateBaseNode = entry.getValue();
                        List<AggregateTargetBindProperty> tarProperties;
                        tarProperties = getTarBindProperty(classMap, new ArrayList<>(List.of(sourcePropertyName)));
                        tarProperties = tarProperties.size() == 0 ? getTarBindProperty(defaultClassMap, new ArrayList<>(List.of(sourcePropertyName))) : tarProperties;
                        if (tarProperties.size() == 0) {
                            //该执行器属性未指定绑定值(可能为无需绑定的类变量)
                            //为减少不必要注解当前执行器里无法区分类变量与期望绑定变量
                            continue;
                        }

                        for (AggregateTargetBindProperty tarProperty : tarProperties) {
                            int nodeType = tarProperty.nodeType;
                            if (nodeType == 0) {
                                String aggregateTargetPropertyName = tarProperty.getAggregateTargetPropertyName();
                                //绑定类型
                                int level = tarProperty.level;
                                //list类型嵌套属性
                                Node firstNode = aggregatePrepare.getDescNode();
                                String[] nodePath = aggregateTargetPropertyName.split("\\.");
                                if (firstNode == null) {
                                    //初始化链路节点(可能从任意节点开始)
                                    List<Node> nodes = new ArrayList<>(List.of(new Node("~", 0, nodeType)));
                                    int index = 0;
                                    while (index < level) {
                                        Node curNode = new Node(nodePath[index], index + 1, nodeType);//todo 位移
                                        nodes.add(curNode);
                                        index++;
                                    }
                                    for (int i = 0; i < nodes.size(); i++) {
                                        Node curNode = nodes.get(i);
                                        if (i == 0) {
                                            firstNode = curNode;
                                            aggregatePrepare.bindNode = firstNode;
                                        }
                                        curNode.prev = i == 0 ? null : nodes.get(i - 1);
                                        if (i + 1 < nodes.size()) {
                                            curNode.next.add(nodes.get(i + 1));
                                        }
                                    }
                                }

                                //遍历链路获取当前Level对应的节点
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
                                        Node newNode = new Node(nodePath[i], i + 1, nodeType);
                                        newNode.prev = tarNode;
                                        tarNode.next.add(newNode);
                                        tarNode = newNode;
                                    }
                                }

                                if (!tarProperty.primary.equals("")) {
                                    AggregateBaseNode primaryNode = aggregatePrepare.aggregateSourceNode.propertyAggregateMap.get(tarProperty.primary);
                                    primaryNode.targetPropertyPath = aggregateTargetPropertyName;
                                    if (primaryNode == null) {
                                        log.error("数据聚合-指定关联Id异常,聚合对象={},绑定属性={}", sourceClass.getName(), sourceClass.getName(), tarProperty.primary);
                                        throw new BusinessException(120_000, "数据聚合-指定关联Id异常");
                                    }
                                    aggregatePrepare.aggregateSourceNode.primaryAggregateBaseNode = primaryNode;
                                }
                                aggregatePrepare.addCommonPrepareNode(tarNode, aggregateBaseNode.initVal(aggregateTargetPropertyName, tarProperty.isRequired(), tarProperty.primary));
                            } else {
                                //映射类型
                                int level = tarProperty.level;//todo 这里的level是聚合对象的?
                                //list类型嵌套属性
                                Node firstNode = aggregatePrepare.mappingNode;
                                String[] nodePath = tarProperty.actuatorPropertyName.split("\\.");
                                if (firstNode == null) {
                                    //初始化链路节点(可能从任意节点开始)
                                    List<Node> nodes = new ArrayList<>(List.of(new Node("~", 0, nodeType)));
                                    int index = 0;
                                    while (index < level) {
                                        Node curNode = new Node(nodePath[index], index + 1, nodeType);//todo 位移
                                        nodes.add(curNode);
                                        index++;
                                    }
                                    for (int i = 0; i < nodes.size(); i++) {
                                        Node curNode = nodes.get(i);
                                        if (i == 0) {
                                            firstNode = curNode;
                                            aggregatePrepare.mappingNode = firstNode;
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
                                        Node newNode = new Node(nodePath[i], i + 1, nodeType);
                                        newNode.prev = tarNode;
                                        tarNode.next.add(newNode);
                                        tarNode = newNode;
                                    }
                                }

                                aggregatePrepare.addCommonPrepareNode(tarNode, aggregateBaseNode.initVal(tarProperty.getAggregateTargetPropertyName(), tarProperty.isRequired(), tarProperty.primary));
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
        AggregateBaseNode primaryAggregateBaseNode;
        final Class<?> sourceClass;
        //待注入值的属性
        final List<String> ignorePropertyList = new ArrayList<>();
        //待反写的属性
        final List<String> allowPropertyList = new ArrayList<>();
        //key  自身相对属性名 val 读写方法
        //todo 支持绑定对象重载?
        //执行器属性名,读写对象map
        final Map<String, AggregateBaseNode> propertyAggregateMap = new HashMap<>();
        //key 自身相对属性名 val spring托管Bean
        //执行器中需要注入spring托管对象Map
        final Map<String, Object> resourcePropertyMap = new HashMap<>();
        Set<AggregateBaseNode> waitResetBaseNodes = new HashSet<>();

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

        public AbstractDataAggregate getEmptyDataAggregate() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            return (AbstractDataAggregate) sourceClass.getDeclaredConstructor().newInstance();
        }

        public void clear() {
            waitResetBaseNodes.clear();
        }
    }

    //todo 此结构多余,可以直接初始化为initAggregateNode()?
    static class AggregateTargetBindProperty {
        /**
         * 标识节点类别
         * 0-绑定
         * 1-映射
         */
        private final int nodeType;
        private String actuatorClassName = "DEFAULT_CLASS_NAME";
        //执行器相对属性名
        private final String actuatorPropertyName;
        //聚合对象相对属性名
        private final String aggregateTargetPropertyName;
        //标示绑定到执行器的属性是否必要
        private final boolean required;
        private final String primary;
        //list类型嵌套属性的层级(非List下的普通属性为0)
        private int level;

        public AggregateTargetBindProperty(String v1, String v2, boolean v3, int v5, String v6, String v7, int level) {
            this.actuatorPropertyName = v1;
            this.aggregateTargetPropertyName = v2;
            this.required = v3;
            this.nodeType = v5;
            this.primary = v7;
            actuatorClassName = v6 == null ? actuatorClassName : v6;

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
    }

    /**
     * 执行器属性被绑定分布
     */
    static class AggregatePrepare {
        private final AggregateSourceNode aggregateSourceNode;

        //执行器属性绑定描述节点
        private Node bindNode;
        private Node lastNode;

        private Node mappingNode;

        private Map<String, Node> nodeMap = new HashMap<>();

        public AggregatePrepare(AggregateSourceNode var) {
            this.aggregateSourceNode = var;
        }

        public void iniAggregatePrepare(Class<?> sourceClass) {
            int size;
            Node topNode;
            List<Node> nextNodes = List.of(bindNode);
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

        public void addCommonPrepareNode(Node tarNode, AggregateBaseNode node) {
            tarNode.commonPrepareNodes.add(node);
        }

        public Node getDescNode() {
            return bindNode;
        }

        public Node getLastNode() {
            return lastNode;
        }
    }

    class AggregateBaseNode {
        private final Field field;
        //执行器属性读写方法
        private final Method readMethod;
        private final Method writeMethod;

        //标示绑定到执行器的属性是否必要
        private boolean required;
        private String primary;
        //执行器对象属性路径
        private String aggregatePropertyPath;
        //聚合对象属性路径
        private String targetPropertyPath;

        //标示属性是否为容器
        private boolean container;
        private Class<? extends Collection> containerType;
        private Class<?> containerClass;
        //所属容器(传递性-容器中泛型对应的属性container都为true)
        //todo 造成循环引用,问题?
        //private AggregateBaseNode belongContainer;

        private boolean initialized = false;
        //属性对应实例
        private Object propertyInstance;
        private Object preInstance;
        private AggregateBaseNode preAggregateBaseNode;
        private List<AggregateBaseNode> subPropertyNode = new ArrayList<>();

        public AggregateBaseNode(Method writeMethod, Method readMethod, Field field, String aggregatePropertyPath) {
            this.field = field;
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;
            this.aggregatePropertyPath = aggregatePropertyPath;
            //todo set类型
            if (field.getType().isAssignableFrom(List.class)) {
                this.container = true;
                this.containerType = ArrayList.class;
                //this.belongContainer = this;
                //todo List多重泛型嵌套测试
                ParameterizedType pType = (ParameterizedType) field.getGenericType();
                if (pType.getActualTypeArguments().length > 0) {
                    containerClass = (Class<?>) pType.getActualTypeArguments()[0];
                }
            }
        }

        public AggregateBaseNode initVal(String targetPropertyPath, boolean required, String primary) {
            this.primary = primary;
            this.required = required;
            this.targetPropertyPath = targetPropertyPath;
            return this;
        }

        public void initAggregateBaseNodeActuator(AggregateBaseNode previousBaseNode) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
            if (containerType != null) {
                propertyInstance = containerType.getDeclaredConstructor().newInstance();
            } else {
                //存在的问题 1.私有化的无参构造方法 2.非包装类型是否要调用?
                propertyInstance = getClassInstance(field.getType());
            }
            if (previousBaseNode != null) {
                this.preAggregateBaseNode = previousBaseNode;
                this.preInstance = previousBaseNode.propertyInstance;
                previousBaseNode.subPropertyNode.add(this);
                if (previousBaseNode.isContainer()) {
                    //将容器下属的泛型解析指向容器本身
                    this.setContainer(true);
                    //this.belongContainer = previousBaseNode.belongContainer;
                    //对于容器类型为其初始化一个元素
                    Object containerClassInstance;
                    Collection preCollection = (Collection) previousBaseNode.propertyInstance;
                    if (preCollection.iterator().hasNext()) {
                        containerClassInstance = preCollection.iterator().next();
                    } else {
                        containerClassInstance = previousBaseNode.containerClass.getDeclaredConstructor().newInstance();
                        preCollection.add(containerClassInstance);
                    }
                    this.writeMethod.invoke(containerClassInstance, propertyInstance);
                } else {
                    this.writeMethod.invoke(previousBaseNode.propertyInstance, propertyInstance);
                }
            }
            initialized = true;
        }

        public void initAggregateBaseNodeTar(AggregateBaseNode previousBaseNode) {
            if (previousBaseNode != null) {
                this.preAggregateBaseNode = previousBaseNode;
                this.preInstance = previousBaseNode.propertyInstance;
                previousBaseNode.subPropertyNode.add(this);
                if (previousBaseNode.isContainer()) {
                    //将容器下属的泛型解析指向容器本身
                    this.setContainer(true);
                }
            }
            initialized = true;
        }

        public void injectNodeVal(Object val, Object instance) throws IllegalAccessException, InvocationTargetException {
            this.writeMethod.invoke(instance, val);
        }

        public <T> T getNodeElementCloneable() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            if (preAggregateBaseNode.isContainer()) {
                if (initialized) {
                    propertyInstance = ((Collection) preAggregateBaseNode.propertyInstance).iterator().next();
                    initialized = false;
                } else {
                    propertyInstance = preAggregateBaseNode.containerClass.getDeclaredConstructor().newInstance();
                    ((Collection) preAggregateBaseNode.propertyInstance).add(propertyInstance);
                }
            } else {
                propertyInstance = field.getType().getDeclaredConstructor().newInstance();
            }
            return (T) propertyInstance;
        }

        private Collection getContainer(Node node, Object source) throws InvocationTargetException, IllegalAccessException {
            if (isContainer()) {
                //todo 根据Node的level和curLevelPropertyName获取上级容器
                AggregateBaseNode curAggregateBaseNode = this;
                while (!node.curLevelPropertyName.equals(curAggregateBaseNode.aggregatePropertyPath)) {
                    if (curAggregateBaseNode.preAggregateBaseNode == null) {
                        throw new BusinessException(120_000, "数据聚合-未找到目标节点");
                    }
                    curAggregateBaseNode = curAggregateBaseNode.preAggregateBaseNode;
                }
                return (Collection) curAggregateBaseNode.readMethod.invoke(source);
            }
            return null;
        }

        public void clear() {
            if (preAggregateBaseNode.isContainer()) {
                ((Collection) preAggregateBaseNode.propertyInstance).clear();
            }
            this.preInstance = null;
            this.propertyInstance = null;
        }

        public boolean isRequired() {
            return required;
        }

        public void setContainer(boolean container) {
            this.container = container;
        }

        public boolean isContainer() {
            return container;
        }

        @Override
        public int hashCode() {
            return writeMethod.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return writeMethod.equals(((AggregateBaseNode) obj).writeMethod);
        }
    }

    private static class Node {
        //节点类型(绑定/映射)
        //node应该与类型无关,buildStatementList()功能复杂导致需要在此传递
        int nodeType;
        Node prev;
        //list属性绑定列表(可能存在多个),不存在List类型时为null,标记作用(每个List中存在任意属性)
        List<Node> next = new ArrayList<>();
        //普通属性绑定列表
        private List<AggregateBaseNode> commonPrepareNodes = new ArrayList<>();
        //key 实际属性访问路径 val 路径下的所有属性
        private Map<String, Map<AggregateBaseNode, Object>> nodeBindValMap = new HashMap<>();

        private String id;
        private int level;
        //当前节点在聚合对象中的属性名
        private String curLevelPropertyName;

        Node(String curLevelPropertyName, int level,int nodeType) {
            this.level = level;
            this.nodeType = nodeType;
            this.id = curLevelPropertyName + "-" + level;
            this.curLevelPropertyName = curLevelPropertyName;
        }

        public boolean addBindVal(AggregateBaseNode prepareNode, Object val, String statement) {
            String key = null;
            if (statement.equals("~")) {
                key = statement;
            } else {
                String[] tarPaths = statement.split("\\.");
                if (tarPaths.length == 1) {
                    if (tarPaths[0].lastIndexOf("]") > -1) {
                        key = tarPaths[0];
                    } else {
                        key = "~";
                    }
                } else if (tarPaths.length > 1) {
                    key = statement;
                }
            }
            if (!nodeBindValMap.containsKey(key)) {
                Map<AggregateBaseNode, Object> map = new HashMap<>();
                map.put(prepareNode, val);
                nodeBindValMap.put(key, map);
            } else {
                nodeBindValMap.get(key).put(prepareNode, val);
            }
            return true;
        }

        private Object getContainer(AggregateSourceNode aggregateSourceNode, Object source) throws InvocationTargetException, IllegalAccessException {
            AggregateBaseNode aggregateBaseNode = aggregateSourceNode.propertyAggregateMap.get(curLevelPropertyName);
            return aggregateBaseNode.readMethod.invoke(source);
        }
    }

}
