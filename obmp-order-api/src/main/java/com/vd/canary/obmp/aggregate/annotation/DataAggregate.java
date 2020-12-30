package com.vd.canary.obmp.aggregate.annotation;

import java.lang.annotation.*;

/**
 * @author zx
 * @date 2020/12/30 16:27
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataAggregate {
}
