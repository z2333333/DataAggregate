package com.vd.canary.aggregate.manytomany.t2;

import com.vd.canary.obmp.aggregate.AbstractOrderDataAggregate;
import lombok.Data;
import org.springframework.data.annotation.Transient;

/**
 * @author zx
 * @date 2021/1/11 15:33
 */
@Data
public class OrderLineSkuImgActuator extends AbstractOrderDataAggregate {

    @Transient
    private String lineId;

    @Transient
    private String businessKey;

    private String imgUrl;

    @Override
    public void doDataAggregate(Object... args) {
        System.out.println("根据商品id和业务key从文件服务获取url...");
        imgUrl = "我是lineId:" + lineId + "的url";
    }
}
