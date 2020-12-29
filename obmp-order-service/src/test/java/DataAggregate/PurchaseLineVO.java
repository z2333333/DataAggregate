package DataAggregate;

import com.vd.canary.obmp.aggregate.annotation.DataAggregatePropertyBind;
import com.vd.canary.obmp.aggregate.annotation.DataAggregateType;
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
@DataAggregateType(classNames = {
        "com.vd.canary.obmp.aggregate.actuator.OrderLineTax",
        "com.vd.canary.obmp.aggregate.actuator.OrderLineAuxiliaryUnit"
})
public class PurchaseLineVO implements Serializable {

    /**
     *  采购订单行ID 
     */
    private String purchaseContractLineId;

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
