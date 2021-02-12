package com.vd.canary.aggregate.manytomany.t4;

import com.vd.canary.obmp.aggregate.AbstractDataAggregate;
import lombok.Data;
import org.springframework.data.annotation.Transient;

/**
 * @author zx
 * @date 2021/1/11 15:33
 */
@Data
public class LineSkuImgActuator extends AbstractDataAggregate {

    @Transient
    private String lineId;

    @Transient
    private String businessKey;

    @Transient
    private String headId;

    @Transient
    private String quoteHeadId;

    private String skuPic;


    @Override
    public void doDataAggregate(Object... args) {
        System.out.println("根据商品id和业务key从文件服务获取url...");
        skuPic = "我是lineId:" + lineId + "的url";
    }
}
