package com.vd.canary.obmp.aggregate.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zx
 * @date 2021/1/19 15:16
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataAggregatePropertyMapping {
    //todo 当前是写死的,属性名发生变化后编译期里无法发现,hutool里好像有对字段引用的包装类
    String value();

//    /* 二选一 */
//
//    /**
//     * 映射执行器属性全限定类名
//     * 适配在服务池中使用的情况
//     * 当前架构下api层不会依赖service,在前者中无法引用后者的class
//     *
//     * @param
//     * @return java.lang.String
//     */
//    String value() default "";
//
//    /**
//     * 映射执行器类
//     * @return
//     */
//    Class<?> type() default Object.class;
//
//    /***
//     * 映射执行器属性对应类中的属性名
//     * 如存在多个
//     *
//     * @param
//     * @return java.lang.String
//     */
//    String property();
}
