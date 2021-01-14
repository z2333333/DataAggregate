package com.vd.canary.aggregate.manytomany;

import com.vd.canary.obmp.order.api.operation.sale.response.ContractAddressInfoVo;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseHeadVO {

    private String purchaseContractHeadId;

    /**
     * 不含税金额
     */
    private BigDecimal noTaxAmount;

    /**
     * 签约日期
     */
    private LocalDateTime signDate;

    /**
     * 订单标准类型
     *0-毛利额 1-毛利率
     */
    private Integer orderGross;

    /**
     * 客户自提地址信息
     */
    private ContractAddressInfoVo selfPickSiteInfo;
}
