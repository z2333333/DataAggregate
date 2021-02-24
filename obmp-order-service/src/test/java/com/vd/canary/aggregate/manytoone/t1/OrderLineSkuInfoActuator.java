package com.vd.canary.aggregate.manytoone.t1;

import com.vd.canary.obmp.aggregate.AbstractOrderDataAggregate;
import lombok.Data;
import org.springframework.data.annotation.Transient;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zx
 * @date 2020/12/24 16:10
 */
@Data
public class OrderLineSkuInfoActuator extends AbstractOrderDataAggregate {

    @Transient
    private SkuQueryReq skuReq;

    private List<ProductInfoVoTest> skuInfos = new ArrayList<>();

    @Override
    public void doDataAggregate(Object... args) {
        System.out.println("根据商品id集合从企业服务获取数据...");
        ProductInfoVoTest productInfoVoTest = new ProductInfoVoTest();
        skuInfos.add(productInfoVoTest);
        productInfoVoTest.setSkuPic("1342294065443917826");
        List<SkuInfoVO> skuInfoVOS = new ArrayList<>();
        productInfoVoTest.setSkuInfoVOS(skuInfoVOS);
        SkuInfoVO s1 = new SkuInfoVO();
        s1.setSkuId("1342294065443917826");
        s1.setTestProperty1("我是商品id:" + s1.getSkuId() + "的辅助单位");
        s1.setTestProperty1("我是商品id:" + s1.getSkuId() + "的辅助数量");
        skuInfoVOS.add(s1);

        ProductInfoVoTest productInfoVoTest1 = new ProductInfoVoTest();
        skuInfos.add(productInfoVoTest1);
        productInfoVoTest1.setSkuPic("1342294065481666561");
        List<SkuInfoVO> skuInfoVOS1 = new ArrayList<>();
        productInfoVoTest.setSkuInfoVOS(skuInfoVOS1);
        SkuInfoVO s2 = new SkuInfoVO();
        s2.setSkuId("1342294065481666561");
        s2.setTestProperty1("我是商品id:" + s1.getSkuId() + "的辅助单位");
        s2.setTestProperty1("我是商品id:" + s1.getSkuId() + "的辅助数量");
        skuInfoVOS.add(s1);
    }
}
