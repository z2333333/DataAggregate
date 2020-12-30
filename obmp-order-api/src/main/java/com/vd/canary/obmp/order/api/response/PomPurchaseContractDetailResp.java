package com.vd.canary.obmp.order.api.response;

import com.vd.canary.obmp.order.api.response.vo.PomPurchaseContractLineVO;
import com.vd.canary.obmp.order.api.response.vo.order.PomPurchaseContractHeadVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @author : zx
 * @date : 2020/4/12
 * @time : 9:42
 */
@Setter
@Getter
public class PomPurchaseContractDetailResp implements Serializable {
	private static final long serialVersionUID = 3697188350855985629L;

	/**
	 * 采购订单信息
	 */
	@ApiModelProperty(value = "采购订单信息")
	private PomPurchaseContractHeadVO pomPurchaseContractHeadVO;
	/**
	 * 销售订单明细表
	 */
	@ApiModelProperty(value = "销售订单明细表")
	private List<PomPurchaseContractLineVO> pomPurchaseContractLineList;

    /**
     * 付款条款
     */
    @ApiModelProperty(value = "付款条款")
    private List<PomPurchaseContractPaymentVO> pomPurchaseContractPaymentVOList;

    /**
     * 结算方式 10=欠款发货 20=款到发货 30=定金发货
     */
    @ApiModelProperty(value = "结算方式")
    private String creditType;
}
