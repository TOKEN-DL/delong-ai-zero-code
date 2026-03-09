package com.token.delongaizerocode.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;


    //在原本错误函数的基础上添加上错误码，消息是错误还是自带
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    //传入自定义好的错误码，还有自定义的错误消息
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    //可以重新制定新的错误消息，提高可拓展性
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}