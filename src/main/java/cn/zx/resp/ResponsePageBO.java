package cn.zx.resp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

/**
 * @author zx
 * @date 2021/3/8 17:55
 */
@Getter
@Setter
public class ResponsePageBO<T> {
    private static final long serialVersionUID = -6227611767220392892L;
    /**
     * 状态：true 成功 or false 失败
     */
    private Boolean success;
    /**
     * 返回数据
     */
    private ResponsePageVO<T> data;
    /**
     * 状态码：200-正常 !=200 错误码
     */
    private Integer code;

    /**
     * 返回消息内容 包括错误消息
     */
    private String message;
    /**
     * 执行错误的详细信息
     */
    private Object error;

    public ResponsePageBO() {
        this.success = false;
        this.code = HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode();
        this.message = HttpResponseStatus.INTERNAL_SERVER_ERROR.getMessage();
    }

    public ResponsePageBO(Integer code, String message) {
        this.success = false;
        this.code = code;
        this.message = message;
    }

    public ResponsePageBO(final Boolean success, ResponseStatus responseStatus) {
        this.success = success;
        this.code = responseStatus.getCode();
        this.message = responseStatus.getMessage();
    }

    public ResponsePageBO(final Boolean success, final ResponsePageVO<T> data, ResponseStatus responseStatus) {
        this.success = success;
        this.data = data;
        this.code = responseStatus.getCode();
        this.message = responseStatus.getMessage();
    }

    /**
     * 是否是失败的
     *
     * @return
     */
    @JsonIgnore
    public boolean isFailed() {
        return !this.success;
    }
}
