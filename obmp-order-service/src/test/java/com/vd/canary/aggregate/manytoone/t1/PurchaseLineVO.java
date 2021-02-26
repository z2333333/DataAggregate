package com.vd.canary.aggregate.manytoone.t1;

import com.vd.canary.obmp.aggregate.annotation.DataAggregatePropertyBind;
import com.vd.canary.obmp.aggregate.annotation.DataAggregatePropertyMapping;
import com.vd.canary.obmp.aggregate.annotation.DataAggregateType;
import com.vd.canary.obmp.aggregate.annotation.TypeProfile;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 采购订单明细-商品表
 * </p>
 *
 * @author : zx  </p>
 * @date : 2020-04-11  </p>
 * @time : 14:36  </p>
 * Created with Canary Automatic Code Generator  </p>
 */
@Data
//@DataAggregateType({
//        LineSkuInfoActuator.class
//})
@DataAggregateType(profile = {
       @TypeProfile(value = LineSkuInfoActuator.class,mode = TypeProfile.Mode.MANY_TO_ONE)
})
public class PurchaseLineVO implements Serializable {

    /**
     * 采购订单行ID
     */
    private String purchaseContractLineId;

    /**
     * 商品ID
     */
    @DataAggregatePropertyBind(value = "skuReq.skuIdList.skuId", className = LineSkuInfoActuator.class, primary = true)
    @DataAggregatePropertyMapping(className = LineSkuInfoActuator.class, value = "orderSkuInfos.skuInfoVOS.skuId")
    private String itemId;

    /**
     * sku图片
     */
    @DataAggregatePropertyMapping(className = LineSkuInfoActuator.class, value = "skuInfos.skuPic")
    private String skuPic;

    /**
     *  税率 
     */
    private String taxCode;

    /**
     *  采购价格 
     */
    private BigDecimal purchasePrice;

    /**
     * 计重方式
     */
    private String weightUnit;
    /**
     * 材质
     */
    private  String textureOfMaterial;

    /**
     * 辅助单位
     */
    private String skuAuxiliaryUnit;

    /**
     * 辅助单位数量
     */
    private BigDecimal skuAuxiliaryNum;

    /**
     * 订单不含税单价
     */
    private BigDecimal noTaxPrice;

    private String testProperty1;
    private String testProperty2;
}
