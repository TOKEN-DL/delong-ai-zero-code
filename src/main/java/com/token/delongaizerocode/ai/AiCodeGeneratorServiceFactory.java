package com.token.delongaizerocode.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.token.delongaizerocode.ai.tools.*;
import com.token.delongaizerocode.exception.BusinessException;
import com.token.delongaizerocode.exception.ErrorCode;
import com.token.delongaizerocode.model.enums.CodeGenTypeEnum;
import com.token.delongaizerocode.service.ChatHistoryService;
import com.token.delongaizerocode.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.spring.AiService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI服务创建工厂
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;


    /**
     * 缓存策略：
     * - 最大缓存1000个实例
     * - 写入后30分钟过期
     * - 访问后10分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI服务实例被移除,缓存键: {}, 原因：{}",key, cause);
            }).build();



    /**
     * 根据appId获取服务
     * @param appId 应用ID
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId){
        //根据appId查找当前缓存，如果没有就调用生成方法
       return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }


    /**
     * 根据appId获取服务
     * @param appId 应用ID
     * @param codeGenType 生成类型
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType){
        String cacheKey = buildCacheKey(appId, codeGenType);

        //根据appId查找当前缓存，如果没有就调用生成方法
        return serviceCache.get(cacheKey,key -> createAiCodeGeneratorService(appId, codeGenType) );
    }



    public AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType){
        log.info("为appId：{} 创建新的AI服务实例", appId);
        //根据appId独立创建的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库中加载对话历史到记忆中
        chatHistoryService.localChatHistoryToMemory(appId, chatMemory, 20);
        return switch (codeGenType){
            //Vue项目生成，使用工具调用和推理模型
            case VUE_PROJECT -> {
                //使用多例模式的StreamingChatModel解决并发问题
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                    .streamingChatModel(reasoningStreamingChatModel)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(toolManager.getAllTools())
                    //处理工具调用幻觉问题
                    .hallucinatedToolNameStrategy(toolExecutionRequest ->
                            ToolExecutionResultMessage.from(toolExecutionRequest,
                                    "Error: there is no tool called "
                                            + toolExecutionRequest.name())
                            )
                    
                    .build();
            }

            case HTML, MULTI_FILE -> {
                //使用多例模式的StreamingChatModel解决并发问题
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield  AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(openAiStreamingChatModel)
                    .chatMemory(chatMemory)
                    .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码生成模型");
        };
    }


    /**
     *创建AI代码生成器服务
     *
     * @return
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService(){
        return getAiCodeGeneratorService(0);
    }


    /**
     *  构造缓存Key
     *
     * @param appId 应用ID
     * @param codeGenType 项目类型
     * @return
     */
    public String buildCacheKey(Long appId, CodeGenTypeEnum codeGenType){
        return appId + "_" + codeGenType.getValue();
    }

}
