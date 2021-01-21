package com.vd.canary.obmp.aggregate.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 属性映射标记注解
 * 仅在聚合对象-执行器N:1时启用
 * @author zx
 * @date 2021/1/19 15:16
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataAggregatePropertyMapping {
    //todo 当前是写死的,属性名发生变化后编译期里无法发现,hutool里好像有对字段引用的包装类

    /**
     * 执行器中返回值映射到聚合对象的标记
     * 多个执行器存在相同属性名时以执行器类名.属性区分或指定className属性
     * @return
     */
    String value();

    /**
     * 标示映射属性所在的执行器
     * 起标示作用,可为空,与value共同定义标记
     * @return
     */
    Class<?> className() default Object.class;

    String classNameStr() default "";
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
