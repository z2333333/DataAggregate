package cn.zx.aggregate.manytomany.t1;

import cn.zx.core.AbstractDataAggregate;
import lombok.Data;
import org.springframework.data.annotation.Transient;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zx
 * @date 2020/12/24 16:10
 */
@Data
public class LineAuxiliaryUnitActuator extends AbstractDataAggregate {

    @Transient
    private String skuId;

    private String testProperty1;
    private String testProperty2;
    private List<OrderFileBillVO> orderFileBillVOs = new ArrayList<>();

    @Override
    public void doDataAggregate(Object... args) {
        System.out.println("根据商品id从企业服务获取辅助单位...");
        testProperty1 = "我是商品Id:"+skuId+"的辅助单位";
        testProperty2 = "我是商品Id:"+skuId+"的辅助数量";
        OrderFileBillVO orderFileBillVO = new OrderFileBillVO();
        orderFileBillVO.setBillId(skuId);
        orderFileBillVO.setFileSize(1l);
        orderFileBillVOs.add(orderFileBillVO);

        OrderFileBillVO orderFileBillVO1 = new OrderFileBillVO();
        orderFileBillVO1.setBillId(skuId);
        orderFileBillVO1.setFileSize(2l);
        orderFileBillVOs.add(orderFileBillVO1);
    }
}
