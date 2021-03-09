package cn.zx.resp;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * @author zx
 * @date 2021/3/8 17:56
 */
@Getter
@Setter
public class ResponsePageVO<T> {
    private static final long serialVersionUID = -2254156511709794827L;
    /**
     * 总数
     */
    private Long totalSize;
    /**
     * 分页数据列表
     */
    private List<T> list;

    /**
     * 分页查询条件
     */
    private Object condition;

    public ResponsePageVO() {

    }

    public ResponsePageVO(RequestPageBO condition) {
        this.totalSize = 0L;
        this.condition = condition;
        this.list = Collections.emptyList();
    }

    public ResponsePageVO(RequestPageBO condition, Long totalSize, List<T> recordList) {
        this.totalSize = totalSize;
        this.condition = condition;
        this.list = recordList;
    }
}
