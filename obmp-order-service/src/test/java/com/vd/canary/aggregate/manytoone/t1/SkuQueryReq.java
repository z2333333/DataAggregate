package com.vd.canary.aggregate.manytoone.t1;

import lombok.Data;

import java.util.List;

/**
 * @author zx
 * @date 2021/1/19 14:18
 */
@Data
public class SkuQueryReq {
    private List<SkuInfoVO> skuIdList;
}
