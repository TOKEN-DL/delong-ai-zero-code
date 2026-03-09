package com.token.delongaizerocode.exception;

/**
 * 抛异常工具简化抛异常的动作，简化try
 */
public class ThrowUtils {

    /**
     *
     * 条件成立则抛出异常
     *
     * @param condition  条件
     * @param runtimeException 异常
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }


    /**
     * 条件成立则抛出异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        //在第一抛异常的基础上抛出了自定义的异常
       throwIf(condition, new BusinessException(errorCode));
    }


    /**
     *
     * 条件成立则抛出异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     * @param message 消息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        //在第一抛异常的基础上抛出了自定义的异常, 自定义了异常消息
        throwIf(condition, new BusinessException(errorCode, message));
    }

}
