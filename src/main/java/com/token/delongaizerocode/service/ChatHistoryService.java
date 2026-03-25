package com.token.delongaizerocode.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.token.delongaizerocode.model.dto.app.AppQueryRequest;
import com.token.delongaizerocode.model.dto.chatthistory.ChatHistoryQueryRequest;
import com.token.delongaizerocode.model.entity.ChatHistory;
import com.token.delongaizerocode.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/TOKEN-DL">时雨夏树</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {


    /**
     *  添加对话历史
     * @param appId   应用id
     * @param message   消息
     * @param messageType  消息类型
     * @param userId  用户id
     * @return 是否成功
     */
    boolean addChatHistory(Long appId,String message, String messageType, Long userId);


    /**
     *  根据应用ID删除记录
     * @param appId 应用ID
     * @return
     */
    boolean deleteByAppId(Long appId);


    /**
     *  加载历史对话到缓存
     *
     * @param appId 应用ID
     * @param chatMemory 历史对话
     * @param maxCount 最大数
     * @return 加载成功多少条
     */
    int localChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 根据查询条件构造数据查询参数
     * @param chatHistoryQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);


    /**
     *  分页查询某app的对话记录
     * @param appId
     * @param lastCreateTime
     * @param loginUser
     * @return
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId,int pageSize, LocalDateTime lastCreateTime, User loginUser);



}
