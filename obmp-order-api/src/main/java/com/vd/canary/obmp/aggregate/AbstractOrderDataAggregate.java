package com.vd.canary.obmp.aggregate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author zx
 * @date 2020/12/16 10:10
 */
public abstract class AbstractOrderDataAggregate implements OrderDataAggregate{

    /**
     * 标示是否执行doDataAggregate方法
     */
    protected volatile boolean actuatorFlag = true;

    protected void init(Map<Method,Object> initMap) throws InvocationTargetException, IllegalAccessException {
        for (Map.Entry<Method, Object> entry : initMap.entrySet()) {
            entry.getKey().invoke(this, entry.getValue());
        }
    }

    public boolean isActuatorFlag() {
        return actuatorFlag;
    }

    public void setActuatorFlag(boolean actuatorFlag) {
        this.actuatorFlag = actuatorFlag;
    }
}
