package com.vd.canary.aggregate.simple;

import com.vd.canary.aggregate.DataAggregateInstance;
import com.vd.canary.core.bo.ResponseBO;
import com.vd.canary.core.util.ResponseUtil;
import com.vd.canary.obmp.aggregate.DataAggregateAOP;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 简单应用场景
 * @author zx
 * @date 2021/1/14 11:44
 */
public class BasicTest {
    private static ResponseBO<SimpleResp> simpleResp;

    @BeforeClass
    public static void setup() {
        SimpleResp resp = new SimpleResp();
        resp.setPurchaseContractHeadId("123");
        simpleResp = ResponseUtil.ok(resp);
    }

    @Test
    public void aggregateBasicTest() {
        DataAggregateAOP dataAggregateAOP = DataAggregateInstance.getInstance();
        dataAggregateAOP.doDataAggregate(simpleResp);
        assertNotNull(simpleResp.getData());
        assertEquals("123", simpleResp.getData().getName());
    }
}
