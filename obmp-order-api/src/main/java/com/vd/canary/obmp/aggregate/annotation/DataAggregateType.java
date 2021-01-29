package com.vd.canary.obmp.aggregate.annotation;

import java.lang.annotation.*;

/**
 * @author zx
 * @date 2020/12/16 13:24
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataAggregateType {
    /* 二选一 */

    /***
     * 执行器类
     *
     * @param
     * @return java.lang.Class[]
     */
    Class[] value() default {};

    /***
     * 执行器全限定类名
     * 适配在服务池中使用的情况
     * 当前架构下api层不会依赖service,在前者中无法引用后者的class
     *
     * @param
     * @return java.lang.String[]
     */
    String[] classNames() default {};

    //todo 提供别名以支持同个聚合对象下相同执行器多次出现
}
