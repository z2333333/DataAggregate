package com.vd.canary.obmp.aggregate;

/**
 * @author zx
 * @date 2020/12/16 15:42
 */
public interface OrderDataAggregate {
    void doDataAggregate(Object... args);
}
