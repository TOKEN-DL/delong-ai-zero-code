package com.token.delongaizerocode.core;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.token.delongaizerocode.ai.model.HtmlFileCodeResult;
import com.token.delongaizerocode.ai.model.MultiFileCodeResult;
import com.token.delongaizerocode.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 文件保存器
 */
@Deprecated
public class CodeFileSaver {

    /**
     * 文件保存的根目录
     */
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";




    // 保存HTML网页代码

    /**
     *
     * 保存HTML网页代码
     *
     * @param htmlFileCodeResult
     * @return
     */
    public static File saveHtmlCodeResult(HtmlFileCodeResult htmlFileCodeResult) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(baseDirPath, "index.html", htmlFileCodeResult.getHtmlCode());
        return new File(baseDirPath);

    }



    // 保存多文件代码


    /**
     *
     * 保存多文件网页代码
     *
     * @param
     * @return
     */
    public static File saveMultiCodeResult(MultiFileCodeResult result) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
        return new File(baseDirPath);

    }


    /**
     *
     *  构建文件的唯一路径： tmp/code_output/biz
     *
     * @param bizType 生成代码类型
     * @return
     */
    // 构建文件的唯一路径： tmp/code_output/biz
    private static String buildUniqueDir(String bizType){
        String uniqueDirName = StrUtil.format("{}+{}", bizType ,IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + bizType + "_" + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }


    // 保存单个文件


    /**
     *  保存单个文件
     * @param dirPath
     * @param filename
     * @param content
     */
    private static void writeToFile(String dirPath, String filename, String content) {
        String filePath = dirPath + File.separator + filename;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }


}
