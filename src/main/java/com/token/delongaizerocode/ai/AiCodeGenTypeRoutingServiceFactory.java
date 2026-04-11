package com.token.delongaizerocode.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.token.delongaizerocode.ai.tools.FileWriteTool;
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
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI服务创建工厂
 */
@Configuration
@Slf4j
public class AiCodeGenTypeRoutingServiceFactory {

    /**
     * 创建AI代码生成类型路由服务实例
     *
     * @return
     */

   public AiCodeGenTypeRoutingService createAiCodeGenTypeRoutingService(){
        ChatModel chatModel = SpringContextUtil.getBean("routingChatModelPrototype", ChatModel.class);
        return AiServices.builder(AiCodeGenTypeRoutingService.class)
               .chatModel(chatModel)
               .build();
   }

    /**
     * 默认提供一个Bean
     * @return
     */
   @Bean
   public AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService(){
       return createAiCodeGenTypeRoutingService();
   }



}
