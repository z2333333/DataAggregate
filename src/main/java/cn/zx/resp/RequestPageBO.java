package cn.zx.resp;

import lombok.Getter;
import lombok.Setter;

import java.util.StringJoiner;

/**
 * @author zx
 * @date 2021/3/8 17:59
 */
@Getter
@Setter
public class RequestPageBO {
    private static final long serialVersionUID = 3697188350855985629L;
    /**
     * 当前页码
     */
    protected Integer pageNum;
    /**
     * 当前展示页面容量 展示条数
     */
    protected Integer pageSize;

    public RequestPageBO() {
        super();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RequestPageBO.class.getSimpleName() + "[", "]")
                .add("pageNum=" + pageNum)
                .add("pageSize=" + pageSize)
                .toString();
    }
}
