package com.vd.canary.obmp.aggregate.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zx
 * @date 2020/12/16 13:43
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataAggregatePropertyBind {
    String value();

    /**
     * 标示绑定到执行器的属性是否必须
     * true-聚合对象中绑定属性为null时将忽略对应执行器
     * false-聚合对象绑定属性为null时依然调用执行器
     */
    boolean required() default true;
}
