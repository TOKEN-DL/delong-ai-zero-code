package com.token.delongaizerocode.ai.model;


import dev.langchain4j.model.output.structured.Description;
import lombok.Data;


/**
 * HTML代码结果
 */
@Description("生成HTML代码文件的结果")
@Data
public class HtmlFileCodeResult {

    /**
     * HTML代码
     */
    @Description("HTML代码")
    private String htmlCode;

    /**
     * 描述
     */
    @Description("生成代码的描述")
    private String description;
}
