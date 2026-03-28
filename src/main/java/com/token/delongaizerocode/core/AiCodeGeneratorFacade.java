package com.token.delongaizerocode.core;

import cn.hutool.json.JSONUtil;
import com.token.delongaizerocode.ai.AiCodeGeneratorService;
import com.token.delongaizerocode.ai.AiCodeGeneratorServiceFactory;
import com.token.delongaizerocode.ai.model.HtmlFileCodeResult;
import com.token.delongaizerocode.ai.model.MultiFileCodeResult;
import com.token.delongaizerocode.ai.model.message.AiResponseMessage;
import com.token.delongaizerocode.ai.model.message.ToolExecutedMessage;
import com.token.delongaizerocode.ai.model.message.ToolRequestMessage;
import com.token.delongaizerocode.core.parser.CodeParseExecutor;
import com.token.delongaizerocode.core.saver.CodeFileSaverExecutor;
import com.token.delongaizerocode.exception.BusinessException;
import com.token.delongaizerocode.exception.ErrorCode;
import com.token.delongaizerocode.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolProviderRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.View;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成门面类，组合代码生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

//    @Resource
//    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    @Autowired
    private View error;


    /**
     *
     * 统一入口： 根据类型生成并保存代码
     *
     * @param userMessage  用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId 应用ID
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId){
        if (codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        //根据appId获取相应的AI服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum){
            case HTML -> {
                HtmlFileCodeResult result = aiCodeGeneratorService.generateHTMLCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }

            default -> {
                String errorMsg = "不支持的生成类型" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.PARAMS_ERROR, errorMsg);
            }
        };
    }


    /**
     *
     * 统一入口： 根据类型生成并保存代码(流式)
     *
     * @param userMessage  用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId){
        if (codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        //根据appId获取相应的AI服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);

        return switch (codeGenTypeEnum){
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHTMLCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);

            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId,userMessage);
                yield processTokenStream(tokenStream);
            }
            default -> {
                String errorMsg = "不支持的生成类型" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.PARAMS_ERROR, errorMsg);
            }
        };
    }

    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                sink.next(JSONUtil.toJsonStr(aiResponseMessage));
            }).onPartialToolExecutionRequest((index, toolExecutionRequest) ->{ //重写源代码适配
                ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                sink.next(JSONUtil.toJsonStr(toolRequestMessage));
            }).onToolExecuted((ToolExecution toolExecution) ->{
                ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
            }).onCompleteResponse((ChatResponse response) -> {
                sink.complete();
            }).onError((Throwable error) -> {
                error.printStackTrace();
                sink.error(error);
            }).start();

        });
    }


    /**
     *  生成 HTML模式的代码并保存
     * @param userMessage 用户提示词
     * @return 保存目录
     */
//    @Deprecated
//    private File generateAndSaveMultiFileCode(String userMessage) {
//
//        MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
//        return CodeFileSaver.saveMultiCodeResult(multiFileCodeResult);
//    }

    /**
     * 生成 多文件模式的代码并保存
     * @param userMessage 用户提示词
     * @return 保存目录
     */
//    @Deprecated
//    private File generateAndSaveHTMLCode(String userMessage) {
//
//        HtmlFileCodeResult htmlFileCodeResult = aiCodeGeneratorService.generateHTMLCode(userMessage);
//        return CodeFileSaver.saveHtmlCodeResult(htmlFileCodeResult);
//    }

    /**
     *  生成 多文件模式的代码并保存(流式)
     * @param userMessage 用户提示词
     * @return 保存目录
     */
//    @Deprecated
//    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
//
//        Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
//        //定义字符串拼接器，用于当流式返回所有的代码之后，再保存代码
//        StringBuilder codeBuilder = new StringBuilder();
//        return result.doOnNext(chunk -> {
//            //实时搜集代码数据片段
//            codeBuilder.append(chunk);
//        }).doOnComplete(() -> {
//            try {
//                //流式返回完成后，保存代码
//                String completeMultiFilCode = codeBuilder.toString();
//                // 解析代码为对象
//                MultiFileCodeResult multiFilCodeResult = CodeParser.parseMultiFileCode(completeMultiFilCode);
//                // 保存代码文件
//                File saveDir = CodeFileSaver.saveMultiCodeResult(multiFilCodeResult);
//                log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
//            }catch (Exception e){
//                log.error("保存失败: {}", e.getMessage());
//            }
//
//
//        });
//    }

    /**
     * 生成 HTML模式的代码并保存 (流式)
     * @param
     * @return 保存目录
     */
    @Deprecated
//    private Flux<String> generateAndSaveHTMLCodeStream(String userMessage) {
//
//        Flux<String> result = aiCodeGeneratorService.generateHTMLCodeStream(userMessage);
//        //定义字符串拼接器，用于当流式返回所有的代码之后，再保存代码
//        StringBuilder codeBuilder = new StringBuilder();
//        return result.doOnNext(chunk -> {
//            //实时搜集代码数据片段
//            codeBuilder.append(chunk);
//        }).doOnComplete(() -> {
//            try {
//                //流式返回完成后，保存代码
//                String completeHtmlCode = codeBuilder.toString();
//                // 解析代码为对象
//                HtmlFileCodeResult htmlFileCodeResult = CodeParser.parseHtmlCode(completeHtmlCode);
//                // 保存代码文件
//                File saveDir = CodeFileSaver.saveHtmlCodeResult(htmlFileCodeResult);
//                log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
//            }catch (Exception e){
//                log.error("保存失败: {}", e.getMessage());
//            }
//
//
//
//        });
//    }


    /**
     *通用流式处理
     * 结合解析执行器和保存执行器
     *
     * @param codeStream 代码流
     * @param codeGenType 代码生成类型
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {

        //定义字符串拼接器，用于当流式返回所有的代码之后，再保存代码
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            //实时搜集代码数据片段
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            try {
                //流式返回完成后，保存代码
                String completeCode = codeBuilder.toString();
                // 解析代码为对象
                Object parsedResult = CodeParseExecutor.executeParser(completeCode, codeGenType);
                // 保存代码文件
                File saveDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
            }catch (Exception e){
                log.error("保存失败: {}", e.getMessage());
            }


        });
    }








}
