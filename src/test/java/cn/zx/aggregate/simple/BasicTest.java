package cn.zx.aggregate.simple;


import cn.zx.aggregate.DataAggregateInstance;
import cn.zx.core.DataAggregateAOP;
import cn.zx.resp.ResponseBO;
import cn.zx.resp.ResponseUtil;
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
