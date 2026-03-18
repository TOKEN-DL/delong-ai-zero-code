package com.token.delongaizerocode.core.parser;

/**
 * 代码解析策略接口    策略模式
 */
public interface CodeParser<T> {

    /**
     *
     * 代码解析内容
     *
     * @param codeContent 原始代码内容
     * @return 解析后的结果对象
     */
    T parseCode(String codeContent);
}
