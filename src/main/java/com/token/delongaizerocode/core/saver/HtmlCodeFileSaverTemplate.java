package com.token.delongaizerocode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.token.delongaizerocode.ai.model.HtmlFileCodeResult;
import com.token.delongaizerocode.exception.BusinessException;
import com.token.delongaizerocode.exception.ErrorCode;
import com.token.delongaizerocode.model.enums.CodeGenTypeEnum;

/**
 * HTML代码文件保存器
 *
 * @author caodelong
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlFileCodeResult> {

    @Override
    public CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlFileCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }


    @Override
    protected void validateInput(HtmlFileCodeResult result) {
        super.validateInput(result);
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码不能为空");
        }
    }
}
