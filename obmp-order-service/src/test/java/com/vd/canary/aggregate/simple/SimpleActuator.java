package com.vd.canary.aggregate.simple;

import com.vd.canary.obmp.aggregate.AbstractOrderDataAggregate;
import lombok.Data;
import org.springframework.data.annotation.Transient;

/**
 * @author zx
 * @date 2021/1/14 13:18
 */
@Data
public class SimpleActuator extends AbstractOrderDataAggregate {

    //数据反写至聚合对象时忽略该属性
    @Transient
    private String id;

    //未被@Transient注解的值将被反写至聚合对象
    private String name;

    @Override
    public void doDataAggregate(Object... args) {
        System.out.println("模拟业务处理...id=" + id);
        name = id;
    }
}
