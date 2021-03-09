package cn.zx.annotations;

import java.lang.annotation.*;

/**
 * @author zx
 * @date 2020/12/16 13:24
 */
@Documented
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeProfile {

    /***
     * 执行器类
     *
     * @param
     * @return java.lang.Class
     */
    Class value() default Object.class;

    /***
     * 执行器全限定类名
     * 适配在服务池中使用的情况
     * 当前架构下api层不会依赖service,在前者中无法引用后者的class
     *
     * @param
     * @return java.lang.String
     */
    String className() default "";

    /**
     * 指定当前执行器模式
     * 默认根据解析结果自动判断,仅在聚合对象与执行器属性多对一时需显示指定
     * 每个聚合对象的执行器几对几对应关系互相独立且有且只有一种状态
     * @return
     */
    Mode mode() default Mode.AUTO;

    //todo 支持同个聚合对象下相同执行器多次出现

    enum Mode {
        /* 聚合对象:执行器 = N:1 */
        MANY_TO_ONE("MANY_TO_ONE"),
        MANY_TO_MANY("MANY_TO_MANY"),
        ONE_TO_ONE("ONE_TO_ONE"),
        AUTO("AUTO");

        public final String value;

        Mode(final String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }
}
