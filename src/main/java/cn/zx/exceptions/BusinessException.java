package cn.zx.exceptions;

import cn.zx.resp.ResponseStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zx
 * @date 2021/3/8 18:11
 */
@Getter
@Setter
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = -5316066944575141688L;
    /**
     * 错误编码
     */
    public int code;
    /**
     * 错误 提示语 提示文案
     */
    public String message;
    /**
     * 错误详情
     *
     */
    public Object error;

    private BusinessException() {
    }

    public BusinessException(final int code, final String message) {
        super("code:" + code + ",message:" + message);
        this.code = code;
        this.message = message;
    }


    public BusinessException(final int code, final String message, final Object error) {
        super("code:" + code + ",message:" + message);
        this.code = code;
        this.message = message;
        this.error = error;
    }


    /**
     * @param responseStatus
     */
    public BusinessException(final ResponseStatus responseStatus) {

        super("code:" + responseStatus.getCode() + ",message:" + responseStatus.getMessage());
        this.code = responseStatus.getCode();
        this.message = responseStatus.getMessage();
    }


    /**
     * @param responseStatus
     * @param error
     */
    public BusinessException(final ResponseStatus responseStatus, final Object error) {
        super("code:" + responseStatus.getCode() + ",message:" + responseStatus.getMessage());
        this.code = responseStatus.getCode();
        this.message = responseStatus.getMessage();
        this.error = error;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public BusinessException append(final String appendMsg) {
        this.message = this.message + appendMsg;
        return this;
    }
    @Override
    public Throwable fillInStackTrace() {
        super.fillInStackTrace();
        final StackTraceElement[] stackTrace = this.getStackTrace();
        this.setStackTrace(stackTrace);
        return this;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        final List<StackTraceElement> stackTraceElementList = new ArrayList<>();
        StackTraceElement[] stackTraceElementArray = super.getStackTrace();

        for (final StackTraceElement stackTraceElement : stackTraceElementArray) {
            stackTraceElementList.add(stackTraceElement);
        }
        stackTraceElementArray = stackTraceElementList.toArray(new StackTraceElement[stackTraceElementList.size()]);
        return stackTraceElementArray;
    }
}
