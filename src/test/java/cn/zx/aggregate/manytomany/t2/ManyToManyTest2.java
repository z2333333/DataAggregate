package cn.zx.aggregate.manytomany.t2;

import cn.zx.aggregate.DataAggregateInstance;
import cn.zx.core.DataAggregateAOP;
import cn.zx.resp.ResponseBO;
import cn.zx.resp.ResponseUtil;
import com.alibaba.fastjson.JSON;
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
public class ManyToManyTest2 {
    private static ResponseBO<InquiryDetailSupplierQuotesInfoTestResp> responseBO;
    private static String text ="{\n" +
            "\t\t\"supplierQuotesInfoVOS\": [{\n" +
            "\t\t\t\t\"quoteHeadId\": \"1306554013853241346\",\n" +
            "\t\t\t\t\"supplierId\": \"53782c4f88e141ad532b0b6ad6f0cdcf\",\n" +
            "\t\t\t\t\"supplierName\": \"GF000005900\",\n" +
            "\t\t\t\t\"supplierCode\": \"zx_test_sup2\",\n" +
            "\t\t\t\t\"supplyEnterpriseType\": \"1\",\n" +
            "\t\t\t\t\"quotesStatus\": \"50\",\n" +
            "\t\t\t\t\"closeReason\": null,\n" +
            "\t\t\t\t\"remark\": null,\n" +
            "\t\t\t\t\"quoteCode\": null,\n" +
            "\t\t\t\t\"expectedDeliveryDate\": 1611816202000,\n" +
            "\t\t\t\t\"validDate\": 1609829211000,\n" +
            "\t\t\t\t\"creditType\": null,\n" +
            "\t\t\t\t\"logisticsFee\": null,\n" +
            "\t\t\t\t\"totalAmount\": 3200.000000,\n" +
            "\t\t\t\t\"fromSource\": null,\n" +
            "\t\t\t\t\"isChoose\": \"10\",\n" +
            "\t\t\t\t\"quoteType\": null,\n" +
            "\t\t\t\t\"supplyQuotesLineVOS\": [{\n" +
            "\t\t\t\t\t\t\"supplyQuoteLineId\": \"1306554013907767601\",\n" +
            "\t\t\t\t\t\t\"supplyQuoteHeadId\": \"1306554013853241346\",\n" +
            "\t\t\t\t\t\t\"demandLineId\": \"1306552545381273602\",\n" +
            "\t\t\t\t\t\t\"quantity\": 100.000,\n" +
            "\t\t\t\t\t\t\"purPrice\": 16.000000,\n" +
            "\t\t\t\t\t\t\"purAmount\": 1600.000000,\n" +
            "\t\t\t\t\t\t\"threeCategoryId\": \"209\",\n" +
            "\t\t\t\t\t\t\"threeCategoryCode\": \"H02013001\",\n" +
            "\t\t\t\t\t\t\"threeCategoryName\": \"建筑材料>其他金属材料>铜板材\",\n" +
            "\t\t\t\t\t\t\"imgUrl\": null\n" +
            "\t\t\t\t\t},\n" +
            "\t\t\t\t\t{\n" +
            "\t\t\t\t\t\t\"supplyQuoteLineId\": \"1306554013907768601\",\n" +
            "\t\t\t\t\t\t\"supplyQuoteHeadId\": \"1306554013853241346\",\n" +
            "\t\t\t\t\t\t\"demandLineId\": \"1306552545381273888\",\n" +
            "\t\t\t\t\t\t\"quantity\": 100.000,\n" +
            "\t\t\t\t\t\t\"purPrice\": 16.000000,\n" +
            "\t\t\t\t\t\t\"purAmount\": 1600.000000,\n" +
            "\t\t\t\t\t\t\"threeCategoryId\": \"209\",\n" +
            "\t\t\t\t\t\t\"threeCategoryCode\": \"H02013001\",\n" +
            "\t\t\t\t\t\t\"threeCategoryName\": \"建筑材料>其他金属材料>铜板材\",\n" +
            "\t\t\t\t\t\t\"imgUrl\": null\n" +
            "\t\t\t\t\t}\n" +
            "\t\t\t\t]\n" +
            "\t\t\t},\n" +
            "\t\t\t{\n" +
            "\t\t\t\t\"quoteHeadId\": \"1306553342756212738\",\n" +
            "\t\t\t\t\"supplierId\": \"1306048943126581250\",\n" +
            "\t\t\t\t\"supplierName\": \"GF000037359\",\n" +
            "\t\t\t\t\"supplierCode\": \"zx_test_sup1\",\n" +
            "\t\t\t\t\"supplyEnterpriseType\": \"1\",\n" +
            "\t\t\t\t\"quotesStatus\": \"50\",\n" +
            "\t\t\t\t\"closeReason\": \"其他\",\n" +
            "\t\t\t\t\"remark\": \"1\",\n" +
            "\t\t\t\t\"quoteCode\": \"1\",\n" +
            "\t\t\t\t\"expectedDeliveryDate\": 1610095575000,\n" +
            "\t\t\t\t\"validDate\": 1610959593000,\n" +
            "\t\t\t\t\"creditType\": null,\n" +
            "\t\t\t\t\"logisticsFee\": 1.000000,\n" +
            "\t\t\t\t\"totalAmount\": 17600.000000,\n" +
            "\t\t\t\t\"fromSource\": \"10\",\n" +
            "\t\t\t\t\"isChoose\": \"10\",\n" +
            "\t\t\t\t\"quoteType\": null,\n" +
            "\t\t\t\t\"supplyQuotesLineVOS\": [{\n" +
            "\t\t\t\t\t\t\"supplyQuoteLineId\": \"1306554013907767600\",\n" +
            "\t\t\t\t\t\t\"supplyQuoteHeadId\": \"1306553342756212738\",\n" +
            "\t\t\t\t\t\t\"demandLineId\": \"1306552545381273602\",\n" +
            "\t\t\t\t\t\t\"quantity\": 100.000,\n" +
            "\t\t\t\t\t\t\"purPrice\": 16.000000,\n" +
            "\t\t\t\t\t\t\"purAmount\": 1600.000000,\n" +
            "\t\t\t\t\t\t\"threeCategoryId\": \"209\",\n" +
            "\t\t\t\t\t\t\"threeCategoryCode\": \"H02013001\",\n" +
            "\t\t\t\t\t\t\"threeCategoryName\": \"建筑材料>其他金属材料>铜板材\",\n" +
            "\t\t\t\t\t\t\"imgUrl\": null\n" +
            "\t\t\t\t\t},\n" +
            "\t\t\t\t\t{\n" +
            "\t\t\t\t\t\t\"supplyQuoteLineId\": \"1306554013907768600\",\n" +
            "\t\t\t\t\t\t\"supplyQuoteHeadId\": \"1306553342756212738\",\n" +
            "\t\t\t\t\t\t\"demandLineId\": \"1306552545381273888\",\n" +
            "\t\t\t\t\t\t\"quantity\": 100.000,\n" +
            "\t\t\t\t\t\t\"purPrice\": 16.000000,\n" +
            "\t\t\t\t\t\t\"purAmount\": 16000.000000,\n" +
            "\t\t\t\t\t\t\"threeCategoryId\": \"209\",\n" +
            "\t\t\t\t\t\t\"threeCategoryCode\": \"H02013001\",\n" +
            "\t\t\t\t\t\t\"threeCategoryName\": \"建筑材料>其他金属材料>铜板材\",\n" +
            "\t\t\t\t\t\t\"imgUrl\": null\n" +
            "\t\t\t\t\t}\n" +
            "\t\t\t\t]\n" +
            "\t\t\t}\n" +
            "\t\t],\n" +
            "\t\t\"totalQuoteNum\": 5,\n" +
            "\t\t\"quoteIngNum\": 0,\n" +
            "\t\t\"quotedNum\": 2,\n" +
            "\t\t\"closedNum\": 3,\n" +
            "\t\t\"selfQuoteNum\": 0\n" +
            "\t}";

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
