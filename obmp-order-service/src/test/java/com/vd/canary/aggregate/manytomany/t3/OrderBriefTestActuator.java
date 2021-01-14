package com.vd.canary.aggregate.manytomany.t3;

import com.vd.canary.obmp.aggregate.AbstractOrderDataAggregate;
import lombok.Data;
import org.springframework.data.annotation.Transient;

/**
 * @author zx
 * @date 2021/1/13 13:54
 */
@Data
public class OrderBriefTestActuator extends AbstractOrderDataAggregate {
    @Transient
    private String skuId;

    private String testProperty1;
    private String testProperty2;

    @Override
    public void doDataAggregate(Object... args) {
        System.out.println("根据商品id从企业服务获取辅助单位...");
        testProperty1 = "我是商品Id:"+skuId+"的辅助单位";
        testProperty2 = "我是商品Id:"+skuId+"的辅助数量";
    }
}
