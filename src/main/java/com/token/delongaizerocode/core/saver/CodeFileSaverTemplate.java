package com.token.delongaizerocode.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.token.delongaizerocode.constant.AppConstant;
import com.token.delongaizerocode.exception.BusinessException;
import com.token.delongaizerocode.exception.ErrorCode;
import com.token.delongaizerocode.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 *抽象代码文件保存器- 模板方法模式
 *
 *
 * @param <T>
 */
public abstract class CodeFileSaverTemplate<T> {


    /**
     * 文件保存的根目录
     */
    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;


    /**
     * 模板方法--保存代码的标准流程
     *
     * @param result 代码结果对象
     * @param appId 应用ID
     * @return 保存目录
     */
    public final File saveCode(T result, Long appId) {

        //1. 验证输入
        validateInput(result);


        //2. 构建唯一目录

        String baseDirPath = buildUniqueDir(appId);

        //3. 保存文件

        saveFiles(result, baseDirPath);


        //4. 返回文件目录对象
        
        return new File(baseDirPath);
    }


    /**
     * 验证输入参数（可由子类覆盖）
     *
     * @param result  代码结果对象
     */
    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码结果对象不能为空");
        }
    }



    /**
     *
     *  构建文件的唯一路径： tmp/code_output/biz
     *
     * @param
     * @return
     */
    // 构建文件的唯一路径： tmp/code_output/biz
    protected String buildUniqueDir(Long appId){
        if (appId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用ID不能为空");
        }
        String codeType = getCodeType().getValue();
        //有修改
        String uniqueDirName = StrUtil.format("{}_{}", codeType , appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator  + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     *  保存单个文件
     * @param dirPath 目录路径
     * @param filename 文件名
     * @param content 文件内容
     */
    public final void writeToFile(String dirPath, String filename, String content) {
        if (StrUtil.isNotBlank(content)) {
            String filePath = dirPath + File.separator + filename;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }

    }





    /**
     *  保存文件
     * @param result 代码结果对象
     * @param baseDirPath 基础目录路径
     */
    protected abstract void saveFiles(T result, String baseDirPath);



    /**
     *
     * 获取生成代码枚举
     *
     * @return 代码生成类型枚举
     */
    public abstract CodeGenTypeEnum getCodeType();



}
