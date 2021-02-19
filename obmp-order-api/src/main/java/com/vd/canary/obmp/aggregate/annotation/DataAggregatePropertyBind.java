package com.vd.canary.obmp.aggregate.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 属性绑定标记
 * 将所注解的属性绑定到执行器对应属性
 * 多个执行器存在相同属性名时以执行器类名.属性区分
 *
 * @author zx
 * @date 2020/12/16 13:43
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataAggregatePropertyBind {
    String value();

    /**
     * 标示绑定到执行器的属性是否必须
     * 为了保持执行器的隔离与灵活性,放到聚合对象属性上而不是执行器上
     * true-聚合对象中绑定属性为null时将忽略对应执行器
     * false-聚合对象绑定属性为null时依然调用执行器
     */
    boolean required() default true;
}
