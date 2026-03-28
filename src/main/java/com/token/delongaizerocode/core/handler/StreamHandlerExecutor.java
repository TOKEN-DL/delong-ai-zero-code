package com.token.delongaizerocode.core.handler;

import com.token.delongaizerocode.model.entity.User;
import com.token.delongaizerocode.model.enums.CodeGenTypeEnum;
import com.token.delongaizerocode.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;


    /**
     * 创建流处理器并处理聊天历史记录
     *
     * @param originFlux  原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @param codeGenType 代码生成类型
     * @return 处理后的流
     */
    public Flux<String> doExecute(Flux<String> originFlux,
                                  ChatHistoryService chatHistoryService,
                                  long appId, User loginUser, CodeGenTypeEnum codeGenType){
        return switch (codeGenType){
            case VUE_PROJECT ->
                    jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser);
            case HTML , MULTI_FILE ->
                    new SimpleTextStreamHandler().handle(originFlux, chatHistoryService, appId, loginUser);
        };
    }
}
