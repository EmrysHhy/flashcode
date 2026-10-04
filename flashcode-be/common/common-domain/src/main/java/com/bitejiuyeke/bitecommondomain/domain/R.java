package com.bitejiuyeke.bitecommondomain.domain;

import lombok.Data;

/**
 * 响应报文封装
 *
 * @param <T> 响应数据
 */
@Data
public class R<T> {

    /**
     * 响应码
     */
    private int code;

    /**
     * 消息
     */
    private String msg;

    /**
     * 数据
     */
    private T data;

    /**
     * 成功响应
     *
     * @return 响应报文
     * @param <T> 数据类型
     */
    public static <T> R<T> ok() {
        return restResult(null, ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg());
    }

    /**
     * 成功响应
     * @param data 响应数据
     * @return 响应报文
     * @param <T>  数据类型
     */
    public static <T> R<T> ok(T data) {
        return restResult(data, ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg());
    }

    /**
     * 成功响应
     * @param data 响应数据
     * @param msg 响应消息
     * @return 响应报文
     * @param <T> 数据类型
     */
    public static <T> R<T> ok(T data, String msg) {
        return restResult(data, ResultCode.SUCCESS.getCode(), msg);
    }

    /**
     * 失败响应
     * @return 响应报文
     * @param <T> 数据类型
     */
    public static <T> R<T> fail() {
        return restResult(null, ResultCode.ERROR.getCode(), ResultCode.ERROR.getMsg());
    }

    /**
     * 失败响应
     * @param msg 响应消息
     * @return 响应报文
     * @param <T> 数据类型
     */
    public static <T> R<T> fail(String msg) {
        return restResult(null, ResultCode.ERROR.getCode(), msg);
    }

    /**
     * 失败响应
     * @param code 响应码
     * @param msg 响应消息
     * @return 响应报文
     * @param <T> 数据类型
     */
    public static <T> R<T> fail(Integer code,String msg) {
        return restResult(null, code, msg);
    }

    /**
     * 失败响应
     * @param data 响应数据
     * @return 响应报文
     * @param <T> 数据类型
     */
    public static <T> R<T> fail(T data) {
        return restResult(data, ResultCode.ERROR.getCode(), ResultCode.ERROR.getMsg());
    }

    /**
     * 失败响应
     * @param data 响应数据
     * @param msg 响应消息
     * @return 响应报文
     * @param <T> 数据类型
     */
    public static <T> R<T> fail(T data, String msg) {
        return restResult(data, ResultCode.ERROR.getCode(), msg);
    }

    /**
     * 失败响应
     * @param code 响应编码
     * @param msg 响应消息
     * @return 响应报文
     * @param <T> 数据类型
     */
    public static <T> R<T> fail(int code, String msg) {
        return restResult(null, code, msg);
    }

    /**
     * 响应结果
     * @param data 响应数据
     * @param code 响应编码
     * @param msg 响应消息
     * @return 响应报文
     * @param <T> 数据类型
     */
    private static <T> R<T> restResult(T data, int code, String msg) {
        R<T> apiResult = new R<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setMsg(msg);
        return apiResult;
    }
}
