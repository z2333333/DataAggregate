package com.vd.canary.aggregate.manytomany.t4;

import lombok.Data;

import java.util.List;

/**
 * @author zx
 * @date 2021/1/19 14:31
 */
@Data
public class SkuInfoBindResp {

    List<SkuInfoVO> skuInfoVOS;
    private String headId;
}
