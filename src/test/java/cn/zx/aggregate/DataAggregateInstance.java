package cn.zx.aggregate;

import cn.zx.core.DataAggregateAOP;

/**
 * @author zx
 * @date 2021/1/14 15:51
 */
public class DataAggregateInstance {
    private DataAggregateInstance() {}

    public static final DataAggregateAOP getInstance(){
        return DataAggregateInstanceHolder.instance;
    }

    private static class DataAggregateInstanceHolder{
        private static DataAggregateAOP instance = new DataAggregateAOP();
    }
}
