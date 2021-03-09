package cn.zx.aggregate.manytomany.t4;

import cn.zx.annotations.DataAggregatePropertyBind;
import cn.zx.annotations.DataAggregateType;
import lombok.Data;

import java.util.List;

/**
 * @author zx
 * @date 2021/1/8 13:21
 */
@Data
@DataAggregateType({
        LineSkuImgActuator.class
})
public class InquiryDetailSupplierQuotesInfoTestResp {

    /**
     * 供方及供方报价信息
     */
    private List<InquiryDetailSupplierQuotesInfoTestVO> supplierQuotesInfoVOS;

    /**
     * 供方及供方报价信息
     */
    //private List<InquiryDetailSupplierQuotesInfoTestVO> supplierQuotesInfoVOS1;
    /**
     * 推送供方合计
     */
    private int totalQuoteNum;

    /**
     * 报价中
     */
    private int quoteIngNum;

    /**
     * 已报价
     */
    private int quotedNum;

    /**
     * 已关闭
     */
    private int closedNum;

    /**
     * 自主报价
     */
    private int selfQuoteNum;

    @DataAggregatePropertyBind("headId")
    private String headId;
}
