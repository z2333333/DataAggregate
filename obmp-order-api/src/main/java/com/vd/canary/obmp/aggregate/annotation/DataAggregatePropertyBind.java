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
     * true-聚合对象中绑定属性为null时将忽略对应执行器
     * false-聚合对象绑定属性为null时依然调用执行器
     */
    boolean required() default true;

    /**
     * 绑定类型
     * 默认根据自动判断,仅在聚合对象与执行器属性多对一时需显示指定
     * 每个聚合对象的执行器几对几对应关系互相独立且有且只有一种状态
     * @return
     */
    BindType type() default BindType.AUTO;

    enum BindType {
        /* 聚合对象:执行器 = N:1 */
        MANY_TO_ONE("MANY_TO_ONE"),
        MANY_TO_MANY("MANY_TO_MANY"),
        ONE_TO_ONE("ONE_TO_ONE"),
        AUTO("AUTO");

        public final String value;

        BindType(final String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }
}
