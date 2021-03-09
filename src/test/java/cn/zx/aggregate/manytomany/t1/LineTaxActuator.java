package cn.zx.aggregate.manytomany.t1;

import cn.zx.core.AbstractDataAggregate;
import lombok.Data;
import org.springframework.data.annotation.Transient;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author zx
 * @date 2020/12/15 15:08
 */
@Data
public class LineTaxActuator extends AbstractDataAggregate {

    /**
     * 订单不含税单价
     */
    private BigDecimal noTaxPrice;

    @Transient
    private String tax;

    @Transient
    private BigDecimal orderTaxPrice;

    @Override
    public void doDataAggregate(Object... args) {
        System.out.println("计算商品不含税单价...");
        //不含税单价＝含税单价/（1＋税率)
        noTaxPrice = orderTaxPrice.divide(BigDecimal.ONE.add(new BigDecimal(tax).divide(new BigDecimal(100))), 2,
                RoundingMode.HALF_UP);
    }
}
