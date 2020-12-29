package com.vd.canary.obmp.aggregate.actuator;

import com.vd.canary.obmp.aggregate.AbstractOrderDataAggregate;
import com.vd.canary.obmp.order.api.constants.PomConstants;
import lombok.Data;
import org.springframework.data.annotation.Transient;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author zx
 * @date 2020/12/15 15:08
 */
@Data
public class OrderLineTax extends AbstractOrderDataAggregate {

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
        //不含税单价＝含税单价/（1＋税率)
        noTaxPrice = orderTaxPrice.divide(BigDecimal.ONE.add(new BigDecimal(tax).divide(new BigDecimal(100))), PomConstants.SCALE_AMOUNT,
                                          RoundingMode.HALF_UP);
    }
}
