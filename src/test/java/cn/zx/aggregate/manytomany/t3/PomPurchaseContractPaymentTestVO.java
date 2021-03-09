package cn.zx.aggregate.manytomany.t3;

import cn.zx.annotations.DataAggregatePropertyBind;
import cn.zx.annotations.DataAggregateType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author zx
 * @date 2020/4/17 19:44
 */
@Data
@DataAggregateType({
        BriefTestActuator.class
})
public class PomPurchaseContractPaymentTestVO {
    /**
     * 付款条款ID
     */
    @DataAggregatePropertyBind("skuId")
    private String contractPaymentId;
    /**
     * 条款号
     */
    private String contractPaymentCode;
    /**
     * 采购订单ID
     */
    private String purchaseContractHeadId;
    /**
     * 付款条款类型
     */
    private String paymentTerm;
    /**
     * 起计时间
     */
    private LocalDateTime beginDate;
    /**
     * 金额占比
     */
    private BigDecimal amountRate;
    /**
     * 金额
     */
    private BigDecimal amount;
    /**
     * 付款依据
     */
    private String paymentCondition;
    /**
     * 付款天数
     */
    private String paymentDays;
    /**
     * 日利率
     */
    private BigDecimal dayProfit;
    /**
     * 付款条款说明
     */
    private String paymentRemark;
    /**
     * 协议条款ID
     */
    private String agreementPaymentId;
    /**
     * 部门ID
     */
    private Integer deptId;
    /**
     * 是否暂停计息，0关闭，1是开启
     */
    private Integer isStopRate;
    /**
     * 起计依据
     */
    private String conditionType;

    private String testProperty1;
    private String testProperty2;
}
