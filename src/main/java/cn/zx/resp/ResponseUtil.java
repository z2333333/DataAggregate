package cn.zx.resp;

import cn.zx.exceptions.BusinessException;

/**
 * @author zx
 * @date 2021/3/8 18:29
 */
public class ResponseUtil {
    /**
     * 业务执行成功
     *
     * @return ResponseBO
     */
    public static <T> ResponseBO<T> ok() {
        return ok(null, HttpResponseStatus.OK.getCode(), HttpResponseStatus.OK.getMessage());
    }

    /**
     * 业务执行成功
     *
     * @param responseStatus 业务错误状态定义
     * @return
     */
    @Deprecated
    public static <T> ResponseBO<T> ok(final ResponseStatus responseStatus) {
        return ok(null, responseStatus.getCode(), responseStatus.getMessage());
    }

    /**
     * 业务执行成功
     *
     * @param message 执行成功 文案
     * @return
     */
    @Deprecated
    public static <T> ResponseBO<T> ok(final String message) {
        return ok(null, HttpResponseStatus.OK.getCode(), message);
    }

    ///**
    // * 业务逻辑判断 返回 boolean 值
    // *
    // * @param success
    // * @return
    // */
    //public static ResponseBO<Boolean> ok(final Boolean bool) {
    //    return new ResponseBO<>(true, bool, HttpResponseStatus.OK.getCode(), HttpResponseStatus.OK.getMessage());
    //}
    //
    ///**
    // * 业务执行成功 返回 Long 直接返回日期也应转为时间戳
    // *
    // * @param number
    // * @return
    // */
    //public static ResponseBO<Long> ok(final Long number) {
    //    return new ResponseBO<>(true, number, HttpResponseStatus.OK.getCode(), HttpResponseStatus.OK.getMessage());
    //}
    //
    ///**
    // * 业务执行成功 返回 number
    // *
    // * @param number
    // * @return
    // */
    //public static ResponseBO<Integer> ok(final Integer number) {
    //    return new ResponseBO<>(true, number, HttpResponseStatus.OK.getCode(), HttpResponseStatus.OK.getMessage());
    //}
    //
    ///**
    // * 业务执行成功 返回 bigDecimal
    // *
    // * @param bigDecimal
    // * @return
    // */
    //public static ResponseBO<BigDecimal> ok(final BigDecimal bigDecimal) {
    //    return new ResponseBO<>(true, bigDecimal, HttpResponseStatus.OK.getCode(), HttpResponseStatus.OK.getMessage());
    //}
    //
    ///**
    // * 业务执行成功 返回字符串
    // *
    // * @param data
    // * @return
    // */
    //public static ResponseBO<String> ok(final String data) {
    //    return ok(data, HttpResponseStatus.OK.getCode(), HttpResponseStatus.OK.getMessage());
    //}
    ///**
    // * 业务执行成功
    // *
    // * @param message 执行成功 文案
    // * @return
    // */
    //public static <T> ResponseBO<T> ok(final String data) {
    //    return ok(null, HttpResponseStatus.OK.getCode(), message);
    //}

    /**
     * 业务执行成功
     *
     * @param data    返回数据
     * @param message 返回消息
     * @param <T>
     * @return
     */
    public static <T> ResponseBO<T> ok(final T data, final String message) {
        return ok(data, HttpResponseStatus.OK.getCode(), message);
    }

    /**
     * 业务执行成功
     *
     * @param data 返回数据
     * @param <T>
     * @return
     */
    public static <T> ResponseBO<T> ok(final T data) {
        return ok(data, HttpResponseStatus.OK.getCode(), HttpResponseStatus.OK.getMessage());
    }

    /**
     * 业务执行成功
     *
     * @param data           返回数据
     * @param responseStatus 业务错误状态定义
     * @param <T>
     * @return
     */
    @Deprecated
    public static <T> ResponseBO<T> ok(final T data, final ResponseStatus responseStatus) {
        return ok(data, responseStatus.getCode(), responseStatus.getMessage());
    }

    /**
     * 业务执行成功
     *
     * @param data    返回数据
     * @param code    错误码
     * @param message 返回消息
     * @param <T>
     * @return
     */
    @Deprecated
    public static <T> ResponseBO<T> ok(final T data, final Integer code, final String message) {
        return new ResponseBO<>(true, data, code, message,null);
    }

    /**
     * 业务执行失败
     *
     * @return
     * @deprecated 不要使用 无参的 failed 方法
     */
    public static <T> ResponseBO<T> failed() {
        throw new BusinessException(HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode(),
                HttpResponseStatus.INTERNAL_SERVER_ERROR.getMessage());
    }

    /**
     * 业务执行失败
     *
     * @param code    错误码
     * @param message 返回消息
     * @param error   错误调试消息
     * @return
     */

    public static <T> ResponseBO<T> failed(final Integer code, final String message, Object error) {
        throw new BusinessException(code, message, error);
    }

    /**
     * 业务执行失败
     *
     * @param code    错误码
     * @param message 返回消息
     * @return
     */
    public static <T> ResponseBO<T> failed(final Integer code, final String message) {
        throw new BusinessException(code, message);
    }


    /**
     * 业务执行失败
     *
     * @param responseStatus 业务错误状态定义
     */
    public static <T> ResponseBO<T> failed(final ResponseStatus responseStatus) {
        throw new BusinessException(responseStatus.getCode(), responseStatus.getMessage());
    }

    /**
     * 业务执行失败
     *
     * @param responseStatus 业务错误状态定义
     * @param error          错误调试消息
     */
    public static <T> ResponseBO<T> failed(final ResponseStatus responseStatus, Object error) {
        throw new BusinessException(responseStatus.getCode(), responseStatus.getMessage(), error);
    }
}
