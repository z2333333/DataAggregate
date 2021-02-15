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
 * 复杂场景测试4
 * 特点:
 * 1.绑定属性位于多级List嵌套容器中且跨对象(list)绑定
 *
 * @author zx
 * @date 2021/1/14 16:23
 */
public class ManyToManyTest4 {
    private static ResponseBO<InquiryDetailSupplierQuotesInfoTestResp> responseBO;
    private static String text = "{\"closedNum\":3,\"headId\":\"headId\",\"quoteIngNum\":0,\"quotedNum\":2,\"selfQuoteNum\":0,\"supplierQuotesInfoVOS\":[{\"expectedDeliveryDate\":\"2021-01-28T14:43:22\",\"inquiryStatus\":\"31\",\"isChoose\":\"20\",\"logisticsFee\":1000.000000,\"quoteHeadId\":\"1306554013853241346\",\"quotesStatus\":\"50\",\"supplierCode\":\"zx_test_sup2\",\"supplierId\":\"53782c4f88e141ad532b0b6ad6f0cdcf\",\"supplierName\":\"GF000005900\",\"supplyEnterpriseType\":\"1\",\"supplyQuotesLineVOS\":[{\"demandLineId\":\"1306552545381273602\",\"purAmount\":1600.00,\"purPrice\":16.000000,\"quantity\":100.000,\"supplyQuoteHeadId\":\"1306554013853241346\",\"supplyQuoteLineId\":\"1306554013907767601\",\"taxCode\":\"13\",\"threeCategoryCode\":\"H02013001\",\"threeCategoryId\":\"209\",\"threeCategoryName\":\"建筑材料>其他金属材料>铜板材\"},{\"demandLineId\":\"1306552545381273888\",\"purAmount\":1600.00,\"purPrice\":16.000000,\"quantity\":100.000,\"supplyQuoteHeadId\":\"1306554013853241346\",\"supplyQuoteLineId\":\"1306554013907768601\",\"taxCode\":\"13\",\"threeCategoryCode\":\"H02013001\",\"threeCategoryId\":\"209\",\"threeCategoryName\":\"建筑材料>其他金属材料>铜板材\"}],\"totalAmount\":4200.00,\"validDate\":\"2021-01-05T14:46:51\"},{\"closeReason\":\"其他\",\"expectedDeliveryDate\":\"2021-01-08T16:46:15\",\"fromSource\":\"10\",\"inquiryStatus\":\"31\",\"isChoose\":\"20\",\"logisticsFee\":1.000000,\"quoteCode\":\"1\",\"quoteHeadId\":\"1306553342756212738\",\"quotesStatus\":\"50\",\"remark\":\"1\",\"supplierCode\":\"zx_test_sup1\",\"supplierId\":\"1306048943126581250\",\"supplierName\":\"GF000037359\",\"supplyEnterpriseType\":\"1\",\"supplyQuotesLineVOS\":[{\"demandLineId\":\"1306552545381273602\",\"purAmount\":1600.00,\"purPrice\":16.000000,\"quantity\":100.000,\"supplyQuoteHeadId\":\"1306553342756212738\",\"supplyQuoteLineId\":\"1306554013907767600\",\"taxCode\":\"13\",\"threeCategoryCode\":\"H02013001\",\"threeCategoryId\":\"209\",\"threeCategoryName\":\"建筑材料>其他金属材料>铜板材\"},{\"demandLineId\":\"1306552545381273888\",\"purAmount\":16000.00,\"purPrice\":16.000000,\"quantity\":100.000,\"supplyQuoteHeadId\":\"1306553342756212738\",\"supplyQuoteLineId\":\"1306554013907768600\",\"taxCode\":\"13\",\"threeCategoryCode\":\"H02013001\",\"threeCategoryId\":\"209\",\"threeCategoryName\":\"建筑材料>其他金属材料>铜板材\"}],\"totalAmount\":17601.00,\"validDate\":\"2021-01-18T16:46:33\"}],\"totalQuoteNum\":5}";

    @BeforeClass
    public static void setup() {
        InquiryDetailSupplierQuotesInfoTestResp resp = JSON.parseObject(text, InquiryDetailSupplierQuotesInfoTestResp.class);
        responseBO = ResponseUtil.ok(resp);
    }

    @Test
    public void manyToManyTest4() {
        DataAggregateAOP dataAggregateAOP = DataAggregateInstance.getInstance();
        dataAggregateAOP.doDataAggregate(responseBO);

        assertNotNull(responseBO.getData());
        assertNotNull(responseBO.getData().getSupplierQuotesInfoVOS());

        assertEquals("我是lineId:1306554013907767601的url", responseBO.getData().getSupplierQuotesInfoVOS().get(0).getSupplyQuotesLineVOS().get(0).getSkuPic());
        assertEquals("我是lineId:1306554013907768601的url", responseBO.getData().getSupplierQuotesInfoVOS().get(0).getSupplyQuotesLineVOS().get(1).getSkuPic());
        assertEquals("我是lineId:1306554013907767600的url", responseBO.getData().getSupplierQuotesInfoVOS().get(1).getSupplyQuotesLineVOS().get(0).getSkuPic());
        assertEquals("我是lineId:1306554013907768600的url", responseBO.getData().getSupplierQuotesInfoVOS().get(1).getSupplyQuotesLineVOS().get(1).getSkuPic());
    }
}
