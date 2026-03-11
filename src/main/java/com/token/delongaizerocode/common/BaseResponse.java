package com.token.delongaizerocode.common;

import com.token.delongaizerocode.exception.ErrorCode;

import java.io.Serializable;

public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    //响应正常返回正常数据
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }


    //响应正常返回正常数据
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    //返回错误类
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
