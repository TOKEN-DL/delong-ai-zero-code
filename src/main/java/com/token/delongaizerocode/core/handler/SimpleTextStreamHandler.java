package com.token.delongaizerocode.core.handler;

import com.token.delongaizerocode.model.entity.User;
import com.token.delongaizerocode.model.enums.ChatHistoryMessageTypeEnum;
import com.token.delongaizerocode.service.ChatHistoryService;
import reactor.core.publisher.Flux;

public class SimpleTextStreamHandler {


    /**
     * 处理传统流
     * 直接收集完整的文本响应
     *
     * @param originFlux 原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return originFlux
                .map(chunk -> {
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> {
                    String aiResponse = aiResponseBuilder.toString();
                    chatHistoryService.addChatHistory(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(),loginUser.getId());
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败： " + error.getMessage();
                    chatHistoryService.addChatHistory(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(),loginUser.getId());
                });

    }
}
