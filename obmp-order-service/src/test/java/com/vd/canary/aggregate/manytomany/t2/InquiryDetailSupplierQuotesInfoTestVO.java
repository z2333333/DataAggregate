package com.vd.canary.aggregate.manytomany.t2;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class InquiryDetailSupplierQuotesInfoTestVO {

    /**
     * 主键id
     */
    private String quoteHeadId;

    /**
     * 供方id
     */
    private String supplierId;

    /**
     * 供方名称
     */
    private String supplierName;

    /**
     * 供方编码
     */
    private String supplierCode;

    /**
     * 供方企业性质
     */
    private String supplyEnterpriseType;

    /**
     * 报价状态 10=未报价 20=已报价 30=已拒绝
     */
    private String quotesStatus;

    /**
     * 关闭的原因
     */
    private String closeReason;

    /**
     * 备注
     */
    private String remark;

    /**
     * 供方报价code
     */
    private String quoteCode;

    /**
     * 最晚交付日期
     */
    private LocalDateTime expectedDeliveryDate;

    /**
     * 报价有效期
     */
    private LocalDateTime validDate;

    /**
     * 结算方式 10=欠款发货 20=款到发货 30=定金发货 40 现款发货 50 金融账期
     */
    //demand_head-现在没有先隐藏
    private String creditType;

    /**
     * 物流费用
     */
    private BigDecimal logisticsFee;

    /**
     * 报价金额
     */
    //line汇总
    private BigDecimal totalAmount;

    /**
     * 报价来源 10 供方报价 20  特批供方报价  30 自主报价
     */
    private String fromSource;

    /**
     * 是否被选中 10 未选中  20 已选中(10表示不推荐)
     */
    private String isChoose;

    /**
     * 报价方式 10 简易报价 20 offer报价
     */
    private String quoteType;

    /**
     * 供方报价商品明细
     */
    private List<SupplyQuotesLineTestVO> supplyQuotesLineVOS;
}
