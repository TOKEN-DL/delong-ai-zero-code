package com.token.delongaizerocode.core.parser;

import com.token.delongaizerocode.exception.BusinessException;
import com.token.delongaizerocode.exception.ErrorCode;
import com.token.delongaizerocode.model.enums.CodeGenTypeEnum;

/**
 *
 * 代码解析执行器
 * 根据代码生成类型执行相应的解析逻辑
 *
 */
public class CodeParseExecutor {

    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();
    private static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();


    /**
     *
     * 执行代码解析
     *
     * @param codeContent  代码内容
     * @param codeGenTypeEnum 代码生成类型
     * @return 解析结果（HtmlCodeResult 或 MultiFileCodeResult）
     */
    public static Object executeParser(String codeContent, CodeGenTypeEnum codeGenTypeEnum){
            return switch (codeGenTypeEnum) {
                case HTML -> htmlCodeParser.parseCode(codeContent);
                case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
                default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
            };

    }
}
