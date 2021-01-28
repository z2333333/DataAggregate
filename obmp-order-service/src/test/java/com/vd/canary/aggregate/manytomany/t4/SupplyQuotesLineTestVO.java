package com.vd.canary.aggregate.manytomany.t4;

import com.vd.canary.obmp.aggregate.annotation.DataAggregatePropertyBind;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author zx
 * @date 2020/4/21 14:13
 */
@Data
public class SupplyQuotesLineTestVO {

    /**
     * 主键id
     */
    @DataAggregatePropertyBind("lineId")
    private String supplyQuoteLineId;

    private String supplyQuoteHeadId;

    private String demandLineId;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 采购含税单价
     */
    private BigDecimal purPrice;

    /**
     * 采购含税金额
     */
    private BigDecimal purAmount;

    /**
     * 当前商品三级类目ID
     * (用于询报价途径保存上次所选类目信息)
     */
    private String threeCategoryId;

    /**
     * 当前商品三级类目Code
     * (用于询报价途径保存上次所选类目信息)
     */
    private String threeCategoryCode;

    /**
     * 当前商品三级类目name
     *
     */
    private String threeCategoryName;

    /**
     * 税率，不含百分号
     */
    private String taxCode;

    /**
     * 商品图片
     */
    private String skuPic;
}
