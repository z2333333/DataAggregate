package cn.zx.resp;

/**
 * @author zx
 * @date 2021/3/8 17:50
 */
public interface ResponseStatus {
    /**
     * 返回状态码 错误码
     *
     * @return
     */
    Integer getCode();

    /**
     * 返回执行消息
     *
     * @return
     */
    String getMessage();
}
