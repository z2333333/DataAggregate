package cn.zx.aggregate.manytomany.t3;

import cn.zx.aggregate.DataAggregateInstance;
import cn.zx.core.DataAggregateAOP;
import cn.zx.resp.ResponseBO;
import cn.zx.resp.ResponseUtil;
import com.alibaba.fastjson.JSON;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 复杂场景测试1
 * 特点:
 * 1.绑定属性位于多级List嵌套容器中
 * 2.返回值为ResponseBO<List<Resp>>类型
 *
 * @author zx
 * @date 2021/1/14 16:23
 */
public class ManyToManyTest3 {
    private static ResponseBO<List<PomPurchaseContractBriefDetailTestResp>> responseBO;
    private static String text1 = "{\n" +
            "\t\"agreementCode\": \"SA2010150002\",\n" +
            "\t\"agreementId\": \"1316611406244081665\",\n" +
            "\t\"agreementName\": \"测试数据202010151320\",\n" +
            "\t\"alreadyApproveAttachmentNum\": 0,\n" +
            "\t\"attachmentWaitBusinessType\": \"PurchaseHeadWaitApprove\",\n" +
            "\t\"departmentName\": \"admin\",\n" +
            "\t\"grossProfit\": 7.35,\n" +
            "\t\"noTaxAmount\": 71.33,\n" +
            "\t\"orderGrossProfit\": 1.23,\n" +
            "\t\"orderGrossProfitAll\": 0.57,\n" +
            "\t\"orderStatus\": \"10\",\n" +
            "\t\"pomPurchaseContractPaymentVOList\": [{\n" +
            "\t\t\"amount\": 80.60,\n" +
            "\t\t\"amountRate\": 100.00,\n" +
            "\t\t\"contractPaymentCode\": \"111\",\n" +
            "\t\t\"contractPaymentId\": \"1316974347594141697\",\n" +
            "\t\t\"isStopRate\": 0,\n" +
            "\t\t\"paymentCondition\": \"INVOICE\",\n" +
            "\t\t\"paymentDays\": \"123\",\n" +
            "\t\t\"paymentRemark\": \"123123123123\",\n" +
            "\t\t\"paymentTerm\": \"QUALITY_ASSURANCE\",\n" +
            "\t\t\"purchaseContractHeadId\": \"1316629181490405378\"\n" +
            "\t}],\n" +
            "\t\"purchaseContractCode\": \"PU2010150025\",\n" +
            "\t\"purchaseContractHeadId\": \"1316629181490405378\",\n" +
            "\t\"staffCode\": \"001163\",\n" +
            "\t\"staffId\": \"5c30b8d983d910b48ea720fee942e43e\",\n" +
            "\t\"staffName\": \"陈厚林\",\n" +
            "\t\"staffPhone\": \"15990158170\",\n" +
            "\t\"supplierCode\": \"GF000001681\",\n" +
            "\t\"supplierId\": \"53782c4f88e141ad532b0b6ad6f0cdcf\",\n" +
            "\t\"supplierName\": \"广东雄塑科技集团股份有限公司\",\n" +
            "\t\"taxAmount\": 9.27,\n" +
            "\t\"totalAmount\": 80.60,\n" +
            "\t\"waitApproveAttachmentNum\": 0\n" +
            "}";
    private static String text2 = "{\n" +
            "\t\"alreadyApproveAttachmentNum\": 0,\n" +
            "\t\"attachmentWaitBusinessType\": \"PurchaseHeadWaitApprove\",\n" +
            "\t\"grossProfit\": 7.35,\n" +
            "\t\"noTaxAmount\": 71.33,\n" +
            "\t\"orderGrossProfit\": 1.23,\n" +
            "\t\"orderGrossProfitAll\": 0.57,\n" +
            "\t\"orderStatus\": \"10\",\n" +
            "\t\"pomPurchaseContractPaymentVOList\": [],\n" +
            "\t\"purchaseContractCode\": \"PU2010150033\",\n" +
            "\t\"purchaseContractHeadId\": \"1316670336987033602\",\n" +
            "\t\"supplierCode\": \"GF000001681\",\n" +
            "\t\"supplierId\": \"53782c4f88e141ad532b0b6ad6f0cdcf\",\n" +
            "\t\"supplierName\": \"广东雄塑科技集团股份有限公司\",\n" +
            "\t\"taxAmount\": 9.27,\n" +
            "\t\"totalAmount\": 80.60,\n" +
            "\t\"waitApproveAttachmentNum\": 0\n" +
            "}";

    @BeforeClass
    public static void setup() {
        PomPurchaseContractBriefDetailTestResp p1 = JSON.parseObject(text1, PomPurchaseContractBriefDetailTestResp.class);
        PomPurchaseContractBriefDetailTestResp p2 = JSON.parseObject(text2, PomPurchaseContractBriefDetailTestResp.class);

        List<PomPurchaseContractBriefDetailTestResp> briefDetailResp = new ArrayList<>();
        briefDetailResp.add(p1);
        briefDetailResp.add(p2);

        responseBO = ResponseUtil.ok(briefDetailResp);
//        PomPurchaseContractBriefDetailResp resp = JSON.parseObject(text, InquiryDetailSupplierQuotesInfoTestResp.class);
//        responseBO = ResponseUtil.ok(resp);
    }

    @Test
    public void manyToManyTest3() {
        DataAggregateAOP dataAggregateAOP = DataAggregateInstance.getInstance();
        dataAggregateAOP.doDataAggregate(responseBO);

        assertNotNull(responseBO.getData());
        assertNotNull(responseBO.getData().get(0).getPomPurchaseContractPaymentVOList().get(0));

        assertEquals("我是商品Id:1316974347594141697的辅助单位",responseBO.getData().get(0).getPomPurchaseContractPaymentVOList().get(0).getTestProperty1());
        assertEquals("我是商品Id:1316974347594141697的辅助数量",responseBO.getData().get(0).getPomPurchaseContractPaymentVOList().get(0).getTestProperty2());
    }
}