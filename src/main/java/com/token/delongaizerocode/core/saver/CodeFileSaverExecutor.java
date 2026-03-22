package com.token.delongaizerocode.core.saver;


import com.token.delongaizerocode.ai.model.HtmlFileCodeResult;
import com.token.delongaizerocode.ai.model.MultiFileCodeResult;
import com.token.delongaizerocode.exception.BusinessException;
import com.token.delongaizerocode.exception.ErrorCode;
import com.token.delongaizerocode.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * 代码文件保存执行器
 * 根据代码类型执行相应的保存逻辑
 */
public class CodeFileSaverExecutor {

    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaver = new HtmlCodeFileSaverTemplate();

    private static final MultiFileCodeFileSaverTemplate multiFileCodeFileSaver = new MultiFileCodeFileSaverTemplate();


    /**
     * 执行代码保存
     *
     * @param codeResult 代码结果对象
     * @param codeGenType 代码生成类型
     * @param appId 应用ID
     * @return 保存的目录
     */
    public static File executeSaver(Object codeResult , CodeGenTypeEnum codeGenType, Long appId){
        return switch (codeGenType){
            case HTML -> htmlCodeFileSaver.saveCode((HtmlFileCodeResult) codeResult, appId);
            case MULTI_FILE -> multiFileCodeFileSaver.saveCode((MultiFileCodeResult) codeResult, appId);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持生成代码类型");
        };
    }
}
