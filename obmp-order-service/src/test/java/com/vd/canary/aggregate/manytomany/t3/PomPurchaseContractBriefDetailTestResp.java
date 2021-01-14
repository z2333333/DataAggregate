package com.vd.canary.aggregate.manytomany.t3;

import com.vd.canary.obmp.order.api.constants.PomConstants;
import com.vd.canary.obmp.order.api.response.PomPurchaseContractPaymentVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : zx
 * @date : 2020/4/12
 * @time : 9:42
 */
@Setter
@Getter
public class PomPurchaseContractBriefDetailTestResp implements Serializable {

    /**
     * 采购订单ID
     */
    @ApiModelProperty(value = "采购订单ID")
    private String purchaseContractHeadId;

    /**
     * 采购订单号
     */
    @ApiModelProperty(value = "采购订单号")
    private String purchaseContractCode;

    /**
     * 供方ID
     */
    @ApiModelProperty(value = "供方ID")
    private String supplierId;

    /**
     * 供方编码
     */
    @ApiModelProperty(value = "供方编码")
    private String supplierCode;

    /**
     * 供方名称
     */
    @ApiModelProperty(value = "供方名称")
    private String supplierName;

    /**
     * 供方vip有效期截止
     */
    @ApiModelProperty(value = "供方vip有效期截止")
    private LocalDateTime validEndDate;

    /**
     * 框架协议名称
     */
    @ApiModelProperty(value = "协议名称")
    private String agreementName;

    /**
     * 采购框架协议号
     */
    @ApiModelProperty(value = "采购框架协议号")
    private String agreementCode;

    /**
     * 采购框架协议ID
     */
    @ApiModelProperty(value = "采购框架协议ID")
    private String agreementId;

    /**
     * 采购经理ID
     */
    @ApiModelProperty(value = "采购经理ID")
    private String staffId;

    /**
     * 采购经理Code
     */
    @ApiModelProperty(value = "采购经理Code")
    private String staffCode;

    /**
     * 采购经理名称
     */
    @ApiModelProperty(value = "采购经理名称")
    private String staffName;

    /**
     * 部门 : DEPARTMENT_NAME
     */
    private String departmentName;


    /**
     * 采购经理号码
     */
    @ApiModelProperty(value = "采购经理号码")
    private String staffPhone;

    /**
     * 采购订单状态
     */
    private String orderStatus;

    /**
     * 附件业务Key(待审批附件)
     */
    private String attachmentWaitBusinessType = PomConstants.attachmentType.PURCHASE_HEAD_WAIT_APPROVE.getKey();

    /**
     * 不含税金额
     */
    private BigDecimal noTaxAmount;

    /**
     * 税额
     */
    private BigDecimal taxAmount;

    /**
     * 含税总价
     */
    private BigDecimal totalAmount;

    /**
     * 整单毛利率
     */
    private BigDecimal grossProfit;

    /**
     * 品类毛利标准
     */
    private BigDecimal categoryGrossProfit;

    /**
     * 毛利额标准
     */
    private BigDecimal orderGrossProfit;

    /**
     * 整单毛利额
     */
    private BigDecimal orderGrossProfitAll;

    /**
     * 待审批附件数
     */
    private Integer waitApproveAttachmentNum = 0;

    /**
     * 已审批附件数
     */
    private Integer alreadyApproveAttachmentNum = 0;

    /**
     * 付款条款
     */
    @ApiModelProperty(value = "付款条款")
    private List<PomPurchaseContractPaymentVO> pomPurchaseContractPaymentVOList = new ArrayList<>();
}
