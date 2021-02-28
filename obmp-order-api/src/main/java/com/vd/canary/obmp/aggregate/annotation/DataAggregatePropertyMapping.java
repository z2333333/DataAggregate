package com.vd.canary.obmp.aggregate.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 属性映射标记注解
 * 提供聚合对象与执行器属性自定义映射功能
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

    /**
     * 聚合对象与执行器待反写属性名称不一致时可以指定别名
     * todo 用了mapping后同等与别名,用于附件二次重复?
     * @return
     */
    String alias() default "";

    /**
     * 标示当前绑定值是否为处理结果的主键
     * @return
     */
    boolean primary() default false;
}
