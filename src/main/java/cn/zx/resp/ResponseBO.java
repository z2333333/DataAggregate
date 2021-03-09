package cn.zx.resp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.StringJoiner;

/**
 * @author zx
 * @date 2021/3/8 17:44
 */
@Getter
@Setter
public class ResponseBO<T> {
    /**
     * 状态：true 成功 or false 失败
     */
    private Boolean success;
    /**
     * 返回数据
     */
    private T data;
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

    public ResponseBO() {
        this.success = false;
        this.code = HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode();
        this.message = HttpResponseStatus.INTERNAL_SERVER_ERROR.getMessage();
    }

    public ResponseBO(Integer code, String message) {
        this.success = false;
        this.code = code;
        this.message = message;
    }

    public ResponseBO(Integer code, String message, Object error) {
        this.success = false;
        this.code = code;
        this.message = message;
        this.error = error;
    }

    public ResponseBO(ResponseStatus responseStatus) {
        this.success = false;
        this.code = responseStatus.getCode();
        this.message = responseStatus.getMessage();
    }

    public ResponseBO(ResponseStatus responseStatus, Object error) {
        this.success = false;
        this.code = responseStatus.getCode();
        this.message = responseStatus.getMessage();
        this.error = error;
    }

    public ResponseBO(final T data) {
        this.success = true;
        this.code = HttpResponseStatus.OK.getCode();
        this.message = HttpResponseStatus.OK.getMessage();
    }

    public ResponseBO(final Boolean success, final T data, ResponseStatus responseStatus) {
        this.success = success;
        this.code = responseStatus.getCode();
        this.message = responseStatus.getMessage();
    }

    public ResponseBO(final Boolean success, final T data, final Integer code, final String message) {
        this.success = success;
        this.data = data;
        this.code = code;
        this.message = message;
    }

    public ResponseBO(final Boolean success, final T data, final Integer code, final String message, Object error) {
        this.data = data;
        this.code = code;
        this.message = message;
        this.success = success;
        this.error = error;
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

    @Override
    public String toString() {
        return new StringJoiner(", ", ResponseBO.class.getSimpleName() + "[", "]")
                .add("success=" + success)
                .add("data=" + data)
                .add("code=" + code)
                .add("message='" + message + "'")
                .add("error=" + error)
                .toString();
    }
}
