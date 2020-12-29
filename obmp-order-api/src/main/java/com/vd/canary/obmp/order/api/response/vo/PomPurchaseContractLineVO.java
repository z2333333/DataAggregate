package com.vd.canary.obmp.order.api.response.vo;

import com.vd.canary.obmp.aggregate.annotation.DataAggregatePropertyBind;
import com.vd.canary.obmp.aggregate.annotation.DataAggregateType;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

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
@DataAggregateType(classNames = {
        "com.vd.canary.obmp.aggregate.actuator.OrderLineTax",
        "com.vd.canary.obmp.aggregate.actuator.OrderLineAuxiliaryUnit"
})
public class PomPurchaseContractLineVO implements Serializable {

    /**
     *  采购订单行ID 
     */
    private String purchaseContractLineId;


    /**
     *  采购订单行号 
     */
    private String purchaseContractLineCode;


    /**
     *  采购订单主ID 
     */
    private String purchaseContractHeadId;


    /**
     *  当前版本号int类型 
     */
    private Integer versionNo;


    /**
     *  预计交货期 
     */
    private Date expectedReceiptDate;


    /**
     *  品种ID 
     */
    private String categoryId;


    /**
     *  品种代码 
     */
    private String categoryCode;


    /**
     *  品种名称 
     */
    private String categoryName;


    /**
     *  商品ID 
     */
    private String itemId;


    /**
     *  商品名称 
     */
    private String itemName;


    /**
     *  商品明细ID 
     */
    private String itemLineId;


    /**
     *  商品编码 
     */
    private String itemLineCode;


    /**
     *  商品类型 
     */
    private String itemType;


    /**
     *  行业参数 
     */
    private String itemSpecDesc;


    /**
     *  存货类型 
     */
    private String materialTypeCode;


    /**
     *  品牌 
     */
    private String brandName;


    /**
     *  商品是否锁货 
     */
    private String lockFlag;


    /**
     *  发票名称 
     */
    private String itemInvoiceName;


    /**
     *  金税代码 
     */
    private String goldenTaxCode;


    /**
     *  供货周期 
     */
    private BigDecimal leadTime;


    /**
     *  区域 
     */
    private String areaCode;


    /**
     *  商家代码 
     */
    private String itemLineOutsysCode;


    /**
     *  数量 
     */
    private BigDecimal purchaseQuantity;

    /**
     * 库存数量
     */
    private BigDecimal storeQuantity;

    /**
     *  计量单位 
     */
    private String unitType;


    /**
     *  税率类型 
     */
    private String taxCodeType;


    /**
     *  税率 
     */
    @DataAggregatePropertyBind("tax")
    private String taxCode;


    /**
     *  采购价格 
     */
    @DataAggregatePropertyBind("orderTaxPrice")
    private BigDecimal purchasePrice;


    /**
     * 销售原价格
     */
    private BigDecimal originalPrice;

    /**
     * 采购指导价
     */
    private BigDecimal purchaseGuidePrice;

    /**
     *  不含税金额 
     */
    private BigDecimal noTaxAmount;


    /**
     *  税额 
     */
    private BigDecimal taxAmount;


    /**
     *  含税总金额 
     */
    private BigDecimal totalAmount;


    /**
     *  销售商品清单 
     */
    private String salesContractLineId;


    /**
     *  明细备注 
     */
    private String contractLineRemarks;


    /**
     *  溢装率 
     */
    private BigDecimal moreRate;


    /**
     *  短装率 
     */
    private BigDecimal lessRate;


    /**
     *  派发单明细ID 
     */
    private String solutionLineId;


    /**
     *  来源单据行号 
     */
    private String fromLineCode;


    /**
     *  来源单据行ID 
     */
    private String fromLineId;


    /**
     *  来源类型 
     */
    private String fromType;


    /**
     *  派发单明细NO 
     */
    private String solutionLineCode;


    /**
     *  战略采购价 
     */
    private BigDecimal purPrice1;


    /**
     *  优惠金额 
     */
    private BigDecimal discountAmount;

    /**
     * 体积
     */
    private String volume;
    /**
     * 重量
     */
    private BigDecimal weight;

    /**
     *  部门ID 
     */
    private Integer deptId;

    /**
     * 含税物流单价
     */
    private BigDecimal deliveryCost;
    /**
     * 能否供货：00-待确认 10-可供货  20-不可供货
     */
    private String canSupply;

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
