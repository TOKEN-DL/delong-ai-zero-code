package com.token.delongaizerocode.common;

import com.token.delongaizerocode.exception.ErrorCode;

/**
 *  返回工具类，提供成功调用和失败调用方法
 */
public class ResultUtils {

    /**
     *
     * 成功
     *
     * @param data 数据
     * @return 响应
     * @param <T> 数据类型
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0, data, "ok");
    }


    /**
     *
     * 失败
     *
     * @param errorCode 醋无码
     * @return 响应
     */
    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     *
     * 失败
     *
     * @param code 错误码
     * @param message 错误消息
     * @return 响应
     */
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null , message);
    }


    /**
     *
     * 失败
     *
     * @param errorCode 错误码
     * @param message 错误消息
     * @return 响应
     */
    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null , message);
    }



}
