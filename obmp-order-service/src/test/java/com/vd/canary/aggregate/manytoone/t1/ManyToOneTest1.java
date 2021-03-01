package com.vd.canary.aggregate.manytoone.t1;

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
 *
 *
 * @author zx
 * @date 2021/1/14 16:23
 */
public class ManyToOneTest1 {
    private static ResponseBO<PurchaseDetailResp> responseBO;
    private static String text ="{\n" +
            "\t\t\"pomPurchaseContractHeadVO\": {\n" +
            "\t\t\t\"attachmentAlreadyBusinessType\": \"PurchaseHeadAlreadyApprove\",\n" +
            "\t\t\t\"attachmentWaitBusinessType\": \"PurchaseHeadWaitApprove\",\n" +
            "\t\t\t\"companyType\": \"2\",\n" +
            "\t\t\t\"contractBeginDate\": \"2020-11-03T00:00:00\",\n" +
            "\t\t\t\"contractName\": \"ztest000001\",\n" +
            "\t\t\t\"contractType\": \"18\",\n" +
            "\t\t\t\"creatorName\": \"admin\",\n" +
            "\t\t\t\"currencyCode\": \"CNY\",\n" +
            "\t\t\t\"cusSiteCityCode\": \"3212\",\n" +
            "\t\t\t\"cusSiteCityName\": \"泰州市\",\n" +
            "\t\t\t\"cusSiteCountryCode\": \"86\",\n" +
            "\t\t\t\"cusSiteCountryName\": \"中国\",\n" +
            "\t\t\t\"cusSiteCountyCode\": \"321282\",\n" +
            "\t\t\t\"cusSiteCountyName\": \"靖江市\",\n" +
            "\t\t\t\"cusSiteRegionCode\": \"32\",\n" +
            "\t\t\t\"cusSiteRegionName\": \"江苏省\",\n" +
            "\t\t\t\"customerCode\": \"XF000032228\",\n" +
            "\t\t\t\"customerConsigneetName\": \"理**\",\n" +
            "\t\t\t\"customerConsigneetPhone\": \"15550555055\",\n" +
            "\t\t\t\"customerId\": \"1291190436398292993\",\n" +
            "\t\t\t\"customerName\": \"临沂坤德商贸有限公司\",\n" +
            "\t\t\t\"customerSiteAddress\": \"******\",\n" +
            "\t\t\t\"customerSiteId\": \"1316294834396778498\",\n" +
            "\t\t\t\"deliveryType\": \"10\",\n" +
            "\t\t\t\"dingApproved\": \"N\",\n" +
            "\t\t\t\"fromCode\": \"SC2012250001001\",\n" +
            "\t\t\t\"fromId\": \"1342294062319161346\",\n" +
            "\t\t\t\"fromSource\": \"NORMAL\",\n" +
            "\t\t\t\"fromType\": \"OL_NO_REGULAR\",\n" +
            "\t\t\t\"gmtCreateTime\": \"2020-12-25T10:20:17\",\n" +
            "\t\t\t\"gmtModifyTime\": \"2020-12-25T10:24:10\",\n" +
            "\t\t\t\"grossProfit\": 7.36,\n" +
            "\t\t\t\"includeLogistics\": true,\n" +
            "\t\t\t\"invoiceMode\": 1,\n" +
            "\t\t\t\"modifierName\": \"admin\",\n" +
            "\t\t\t\"noTaxAmount\": 20.00,\n" +
            "\t\t\t\"orderStatus\": \"17\",\n" +
            "\t\t\t\"projectCode\": \"PR200004\",\n" +
            "\t\t\t\"projectId\": \"1315832387760275457\",\n" +
            "\t\t\t\"projectName\": \"111\",\n" +
            "\t\t\t\"purchaseContractCode\": \"PU2012250001\",\n" +
            "\t\t\t\"purchaseContractHeadId\": \"1342294065091596290\",\n" +
            "\t\t\t\"salesContractCode\": \"SC2012250001001\",\n" +
            "\t\t\t\"salesContractHeadId\": \"1342294062319161346\",\n" +
            "\t\t\t\"supplementaryType\": 1,\n" +
            "\t\t\t\"supplierCode\": \"GF000001681\",\n" +
            "\t\t\t\"supplierId\": \"53782c4f88e141ad532b0b6ad6f0cdcf\",\n" +
            "\t\t\t\"supplierName\": \"广东雄塑科技集团股份有限公司\",\n" +
            "\t\t\t\"taxAmount\": 2.60,\n" +
            "\t\t\t\"totalAmount\": 22.60,\n" +
            "\t\t\t\"transportType\": 2,\n" +
            "\t\t\t\"versionNo\": 1\n" +
            "\t\t},\n" +
            "\t\t\"pomPurchaseContractLineList\": [{\n" +
            "\t\t\t\"brandName\": \"雄塑\",\n" +
            "\t\t\t\"canSupply\": \"00\",\n" +
            "\t\t\t\"categoryCode\": \"H04008001\",\n" +
            "\t\t\t\"categoryId\": \"464\",\n" +
            "\t\t\t\"categoryName\": \"塑料管\",\n" +
            "\t\t\t\"deliveryCost\": 0.000000,\n" +
            "\t\t\t\"expectedReceiptDate\": 1604419200000,\n" +
            "\t\t\t\"fromLineCode\": \"20122500010010001\",\n" +
            "\t\t\t\"fromLineId\": \"1342294062843449346\",\n" +
            "\t\t\t\"fromType\": \"OL_NO_REGULAR\",\n" +
            "\t\t\t\"itemId\": \"1255464985006567426\",\n" +
            "\t\t\t\"itemLineCode\": \"H04008001S0011SP00002\",\n" +
            "\t\t\t\"itemName\": \"雄塑PVC排水管H04008001S0011SP00002\",\n" +
            "\t\t\t\"itemSpecDesc\": \"φ75/3寸   2.3mm;\",\n" +
            "\t\t\t\"itemType\": \"STOCK\",\n" +
            "\t\t\t\"lessRate\": 1.000000,\n" +
            "\t\t\t\"moreRate\": 1.000000,\n" +
            "\t\t\t\"noTaxAmount\": 7.13,\n" +
            "\t\t\t\"originalPrice\": 8.700000,\n" +
            "\t\t\t\"purchaseContractHeadId\": \"1342294065091596290\",\n" +
            "\t\t\t\"purchaseContractLineCode\": \"PU2012250001001\",\n" +
            "\t\t\t\"purchaseContractLineId\": \"1342294065443917826\",\n" +
            "\t\t\t\"purchaseGuidePrice\": 8.060000,\n" +
            "\t\t\t\"purchasePrice\": 8.060000,\n" +
            "\t\t\t\"purchaseQuantity\": 1.000000,\n" +
            "\t\t\t\"salesContractLineId\": \"1342294062843449346\",\n" +
            "\t\t\t\"taxAmount\": 0.93,\n" +
            "\t\t\t\"taxCode\": \"13\",\n" +
            "\t\t\t\"taxCodeType\": \"TAX\",\n" +
            "\t\t\t\"totalAmount\": 8.06,\n" +
            "\t\t\t\"unitType\": \"米\",\n" +
            "\t\t\t\"versionNo\": 1\n" +
            "\t\t}, {\n" +
            "\t\t\t\"brandName\": \"雄塑\",\n" +
            "\t\t\t\"canSupply\": \"00\",\n" +
            "\t\t\t\"categoryCode\": \"H04008001\",\n" +
            "\t\t\t\"categoryId\": \"464\",\n" +
            "\t\t\t\"categoryName\": \"塑料管\",\n" +
            "\t\t\t\"deliveryCost\": 0.000000,\n" +
            "\t\t\t\"expectedReceiptDate\": 1605110400000,\n" +
            "\t\t\t\"fromLineCode\": \"20122500010010002\",\n" +
            "\t\t\t\"fromLineId\": \"1342294063166410753\",\n" +
            "\t\t\t\"fromType\": \"OL_NO_REGULAR\",\n" +
            "\t\t\t\"itemId\": \"1255464985690238978\",\n" +
            "\t\t\t\"itemLineCode\": \"H04008001S0011SP00003\",\n" +
            "\t\t\t\"itemName\": \"雄塑PVC排水管φ110/4寸  3.2mm\",\n" +
            "\t\t\t\"itemSpecDesc\": \"φ110/4寸  3.2mm;\",\n" +
            "\t\t\t\"itemType\": \"STOCK\",\n" +
            "\t\t\t\"lessRate\": 2.000000,\n" +
            "\t\t\t\"moreRate\": 3.000000,\n" +
            "\t\t\t\"noTaxAmount\": 12.87,\n" +
            "\t\t\t\"originalPrice\": 15.700000,\n" +
            "\t\t\t\"purchaseContractHeadId\": \"1342294065091596290\",\n" +
            "\t\t\t\"purchaseContractLineCode\": \"PU2012250001002\",\n" +
            "\t\t\t\"purchaseContractLineId\": \"1342294065481666561\",\n" +
            "\t\t\t\"purchaseGuidePrice\": 14.540000,\n" +
            "\t\t\t\"purchasePrice\": 14.540000,\n" +
            "\t\t\t\"purchaseQuantity\": 1.000000,\n" +
            "\t\t\t\"salesContractLineId\": \"1342294063166410753\",\n" +
            "\t\t\t\"taxAmount\": 1.67,\n" +
            "\t\t\t\"taxCode\": \"13\",\n" +
            "\t\t\t\"taxCodeType\": \"TAX\",\n" +
            "\t\t\t\"totalAmount\": 14.54,\n" +
            "\t\t\t\"unitType\": \"米\",\n" +
            "\t\t\t\"versionNo\": 1\n" +
            "\t\t}],\n" +
            "\t\t\"pomPurchaseContractPaymentVOList\": []\n" +
            "\t}";

    @BeforeClass
    public static void setup() {
        PurchaseDetailResp resp = JSON.parseObject(text, PurchaseDetailResp.class);
        responseBO = ResponseUtil.ok(resp);
    }

    @Test
    public void manyToManyTest1() {
        DataAggregateAOP dataAggregateAOP = DataAggregateInstance.getInstance();
        dataAggregateAOP.doDataAggregate(responseBO);

        assertNotNull(responseBO.getData());
        assertEquals("我是商品id:1255464985006567426的图片url", responseBO.getData().getPomPurchaseContractLineList().get(0).getSkuPic());
        assertEquals("我是商品id:1255464985690238978的图片url", responseBO.getData().getPomPurchaseContractLineList().get(1).getSkuPic());
    }
}
