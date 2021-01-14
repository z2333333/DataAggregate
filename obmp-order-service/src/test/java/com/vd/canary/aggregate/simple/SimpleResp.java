package com.vd.canary.aggregate.simple;

import com.vd.canary.obmp.aggregate.annotation.DataAggregatePropertyBind;
import com.vd.canary.obmp.aggregate.annotation.DataAggregateType;
import lombok.Data;

/**
 * @author zx
 * @date 2021/1/14 11:47
 */
@Data
/** 指定执行器 */
@DataAggregateType({
        SimpleActuator.class
})
public class SimpleResp {

    //为执行器中名称为"id"的属性注入purchaseContractHeadId所对应的值
    @DataAggregatePropertyBind("id")
    private String purchaseContractHeadId;

    //将接收执行器中相同属性的值
    private String name;
}
