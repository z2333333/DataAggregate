package com.vd.canary.obmp.order.api.operation.demand.response;

import com.vd.canary.obmp.aggregate.annotation.DataAggregatePropertyBind;
import com.vd.canary.obmp.aggregate.annotation.DataAggregateType;
import com.vd.canary.obmp.order.api.constants.DemandConstants;
import com.vd.canary.obmp.order.api.response.vo.order.OrderFileBillVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author xingdongyang
 * @date 2020/7/8
 */
@Data
@DataAggregateType(classNames =
        {"com.vd.canary.obmp.aggregate.actuator.OrderFileSeparateActuator"}
)
public class DemandLineVo implements Serializable {

    /**
     * 需求明细主键
     */
    @DataAggregatePropertyBind("fillId")
    @ApiModelProperty("需求明细主键")
    private String demandLineId;

    /**
     * skuId
     * */
    @ApiModelProperty("skuId")
    private String skuId;

    @ApiModelProperty("skuCode")
    private String skuCode;

    /**
     * 商品名称
     */
    @ApiModelProperty("商品名称")
    private String skuName;

    /**
     * 规格型号
     */
    @ApiModelProperty("规格型号")
    private String specDesc;

    /**
     * 品牌名称
     */
    @ApiModelProperty("品牌名称")
    private String brandName;

    /**
     * 数量
     */
    @ApiModelProperty("数量")
    private BigDecimal quantity;

    /**
     * 单位名称
     */
    @ApiModelProperty("单位名称")
    private String unitName;


    /**
     * 品种ID
     */
    @ApiModelProperty("品种ID")
    private String categoryId;

    /**
     * 品种代码
     */
    @ApiModelProperty("品种代码")
    private String categoryCode;

    /**
     * 品种名称
     */
    @ApiModelProperty("品种名称")
    private String categoryName;

    /**
     * 商品属性
     */
    @ApiModelProperty("商品属性")
    private String skuAttribute;

    /**
     * 商品行文件业务key
     */
    @DataAggregatePropertyBind("businessType")
    private String fileBusinessKey = DemandConstants.DemandFileType.DEMAND_LINE_ANNEX.getCode();

    /**
     * 附件数量
     * */
    private Integer attachmentNum;

    /**
     * 附件信息
     */
    private List<OrderFileBillVO> orderFileBillVOs;
}
