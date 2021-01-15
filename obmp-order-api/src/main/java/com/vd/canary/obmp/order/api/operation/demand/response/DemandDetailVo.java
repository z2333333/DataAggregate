package com.vd.canary.obmp.order.api.operation.demand.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author xingdongyang
 * @date 2020/7/8
 */
@Data
public class DemandDetailVo implements Serializable {
    /**
     * 需求单主表信息
     */
    private DemandHeadVo demandHeadVo;
    /**
     * 需求单商品信息
     */
    private List<DemandLineVo> lineVoList;

    /**
     * 需求单对应的询价单的集合
     */
    private List<InquiryDetailVo> inquiryDetailVo;
}
