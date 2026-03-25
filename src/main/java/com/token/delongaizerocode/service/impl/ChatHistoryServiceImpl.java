package com.token.delongaizerocode.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.token.delongaizerocode.constant.UserConstant;
import com.token.delongaizerocode.exception.ErrorCode;
import com.token.delongaizerocode.exception.ThrowUtils;
import com.token.delongaizerocode.model.dto.chatthistory.ChatHistoryQueryRequest;
import com.token.delongaizerocode.model.entity.App;
import com.token.delongaizerocode.model.entity.ChatHistory;
import com.token.delongaizerocode.mapper.ChatHistoryMapper;
import com.token.delongaizerocode.model.entity.User;
import com.token.delongaizerocode.model.enums.ChatHistoryMessageTypeEnum;
import com.token.delongaizerocode.service.AppService;
import com.token.delongaizerocode.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/TOKEN-DL">时雨夏树</a>
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

    @Resource
    @Lazy
    private AppService appService;



    @Override
    public boolean addChatHistory(Long appId, String message, String messageType, Long userId) {
        //基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不应该为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message) , ErrorCode.PARAMS_ERROR, "消息内容不应该为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType) , ErrorCode.PARAMS_ERROR, "消息类型不应该为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        // 验证消息类型是否有效
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null , ErrorCode.PARAMS_ERROR, "不支持的消息类型");
        //插入数据库
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistory);

    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("app_id", appId);
        return this.remove(queryWrapper);

    }

    @Override
    public int localChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount){

        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if(CollUtil.isEmpty(historyList)){
                return 0;
            }
            //反转列表，确保按照时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            //按照时间顺序添加到记忆中g
            int loadedCount = 0;
            //先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                }
                loadedCount++;
            }
            log.info("成功为appId:{}加载{}条历史消息", appId, loadedCount);
            return loadedCount;

        } catch (Exception e) {
            log.error("加载对话历史失败，appId:{}，error:{}", appId, e.getMessage());
            //加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }



    }




    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        //拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        //游标查询逻辑 - 只使用createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime); // 小于创建时间的数据
        }
        //排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.like(sortField, "ascend".equals(sortField));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;



    }

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId,int pageSize, LocalDateTime lastCreateTime, User loginUser) {
        //校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR, "用户未登录");
        //验证权限，只有创建者和管理员可以看
        App app = appService.getById(appId);
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin ||!isCreator, ErrorCode.NOT_AUTH_ERROR, "无权查看应用对话历史");
        //构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        //查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);


    }
}
