package com.token.delongaizerocode.ai;

import com.token.delongaizerocode.ai.model.HtmlFileCodeResult;
import com.token.delongaizerocode.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {

    /**
     *
     * 生成 HTML 代码
     *
     * @param userMessage 用户提示词
     * @return AI输出的结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlFileCodeResult generateHTMLCode(String userMessage);

    /**
     *
     * 生成多文件代码
     *
     * @param userMessage 用户提示词
     * @return AI输出的结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);



    /**
     *
     * 生成 HTML 代码
     *
     * @param userMessage 用户提示词
     * @return AI输出的结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHTMLCodeStream(String userMessage);

    /**
     *
     * 生成多文件代码
     *
     * @param userMessage 用户提示词
     * @return AI输出的结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);





}
