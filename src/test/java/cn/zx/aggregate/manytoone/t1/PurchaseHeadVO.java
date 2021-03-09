package cn.zx.aggregate.manytoone.t1;

import cn.zx.annotations.DataAggregatePropertyMapping;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseHeadVO {

    @DataAggregatePropertyMapping("orderSkuInfos.headId")
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

    private String testHeadProperty;
}
