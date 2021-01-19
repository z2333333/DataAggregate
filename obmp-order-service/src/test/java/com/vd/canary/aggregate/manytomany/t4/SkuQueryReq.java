package com.vd.canary.aggregate.manytomany.t4;

import lombok.Data;

import java.util.List;

/**
 * @author zx
 * @date 2021/1/19 14:18
 */
@Data
public class SkuQueryReq {
    private List<String> skuIdList;
}
