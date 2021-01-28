package com.vd.canary.aggregate.manytomany.t4;

import com.alibaba.fastjson.JSON;
import com.vd.canary.aggregate.DataAggregateInstance;
import com.vd.canary.core.bo.ResponseBO;
import com.vd.canary.core.util.ResponseUtil;
import com.vd.canary.obmp.aggregate.DataAggregateAOP;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 复杂场景测试1
 * 特点:
 * 1.绑定属性位于多级List嵌套容器中
 * 2.执行器反写时为多对多关系
 *
 * @author zx
 * @date 2021/1/14 16:23
 */
public class ManyToManyTest4 {
    private static ResponseBO<InquiryDetailSupplierQuotesInfoTestResp> responseBO;
    private static String text =" ";

    @BeforeClass
    public static void setup() {
        InquiryDetailSupplierQuotesInfoTestResp resp = JSON.parseObject(text, InquiryDetailSupplierQuotesInfoTestResp.class);
        responseBO = ResponseUtil.ok(resp);
    }

    @Test
    public void manyToManyTest2() {
        DataAggregateAOP dataAggregateAOP = DataAggregateInstance.getInstance();
        dataAggregateAOP.doDataAggregate(responseBO);

        assertNotNull(responseBO.getData());
        assertNotNull(responseBO.getData().getSupplierQuotesInfoVOS());

        assertNotNull(responseBO.getData().getSupplierQuotesInfoVOS().get(0).getSupplyQuotesLineVOS().get(0).getImgUrl());
        assertEquals("我是lineId:1306554013907767601的url",responseBO.getData().getSupplierQuotesInfoVOS().get(0).getSupplyQuotesLineVOS().get(0).getImgUrl());
        assertNotNull(responseBO.getData().getSupplierQuotesInfoVOS().get(0).getSupplyQuotesLineVOS().get(1).getImgUrl());
        assertEquals("我是lineId:1306554013907768601的url",responseBO.getData().getSupplierQuotesInfoVOS().get(0).getSupplyQuotesLineVOS().get(1).getImgUrl());

        assertNotNull(responseBO.getData().getSupplierQuotesInfoVOS().get(1).getSupplyQuotesLineVOS().get(0).getImgUrl());
        assertEquals("我是lineId:1306554013907767600的url",responseBO.getData().getSupplierQuotesInfoVOS().get(1).getSupplyQuotesLineVOS().get(0).getImgUrl());
        assertNotNull(responseBO.getData().getSupplierQuotesInfoVOS().get(1).getSupplyQuotesLineVOS().get(1).getImgUrl());
        assertEquals("我是lineId:1306554013907768600的url",responseBO.getData().getSupplierQuotesInfoVOS().get(1).getSupplyQuotesLineVOS().get(1).getImgUrl());
    }
}
