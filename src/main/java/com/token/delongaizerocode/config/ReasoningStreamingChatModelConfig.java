package com.token.delongaizerocode.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;


    /**
     * 推理模型（用于Vue项目生成，都i工具调用使用）
     *
     * @return
     */
    @Primary
    @Bean
    public StreamingChatModel reasoningSteamingChatModel(){

        //测试使用
        final String modelName = "deepseek-chat";
        final int maxTokens = 8192;
        //生产使用
//        final String modelName = "deepseek-reasoner";
//        final int maxTokens = 32768;

        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logRequests(true)
                .logResponses(true)

                .build();

    }


}
