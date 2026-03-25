package com.token.delongaizerocode.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.token.delongaizerocode.annotation.AuthnCheck;
import com.token.delongaizerocode.common.BaseResponse;
import com.token.delongaizerocode.common.ResultUtils;
import com.token.delongaizerocode.constant.UserConstant;
import com.token.delongaizerocode.exception.ErrorCode;
import com.token.delongaizerocode.exception.ThrowUtils;
import com.token.delongaizerocode.model.dto.chatthistory.ChatHistoryQueryRequest;
import com.token.delongaizerocode.model.entity.User;
import com.token.delongaizerocode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.token.delongaizerocode.model.entity.ChatHistory;
import com.token.delongaizerocode.service.ChatHistoryService;

import java.time.LocalDateTime;
import java.util.List;

/**
 *  控制层。
 *
 * @author <a href="https://github.com/TOKEN-DL">时雨夏树</a>
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;


    @Resource
    private UserService userService;


    /**
     *
     * 分页查询某个应用的对话历史
     *
     *
     * @param appId     应用ID
     * @param pageSize   页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request   请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null,ErrorCode.PARAMS_ERROR,"应用ID为空" );
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);

    }

    /**
     *
     *  管理员分页查询对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @AuthnCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAppChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest){
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageSize = chatHistoryQueryRequest.getPageSize();
        long pageNum = chatHistoryQueryRequest.getPageNum();
        //查数据
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);

    }


//    /**
//     * 保存。
//     *
//     * @param chatHistory
//     * @return {@code true} 保存成功，{@code false} 保存失败
//     */
//    @PostMapping("save")
//    public boolean save(@RequestBody ChatHistory chatHistory) {
//        return chatHistoryService.save(chatHistory);
//    }
//
//    /**
//     * 根据主键删除。
//     *
//     * @param id 主键
//     * @return {@code true} 删除成功，{@code false} 删除失败
//     */
//    @DeleteMapping("remove/{id}")
//    public boolean remove(@PathVariable Long id) {
//        return chatHistoryService.removeById(id);
//    }
//
//    /**
//     * 根据主键更新。
//     *
//     * @param chatHistory
//     * @return {@code true} 更新成功，{@code false} 更新失败
//     */
//    @PutMapping("update")
//    public boolean update(@RequestBody ChatHistory chatHistory) {
//        return chatHistoryService.updateById(chatHistory);
//    }
//
//    /**
//     * 查询所有。
//     *
//     * @return 所有数据
//     */
//    @GetMapping("list")
//    public List<ChatHistory> list() {
//        return chatHistoryService.list();
//    }
//
//    /**
//     * 根据主键获取。
//     *
//     * @param id 主键
//     * @return 详情
//     */
//    @GetMapping("getInfo/{id}")
//    public ChatHistory getInfo(@PathVariable Long id) {
//        return chatHistoryService.getById(id);
//    }
//
//    /**
//     * 分页查询。
//     *
//     * @param page 分页对象
//     * @return 分页对象
//     */
//    @GetMapping("page")
//    public Page<ChatHistory> page(Page<ChatHistory> page) {
//        return chatHistoryService.page(page);
//    }
//
}
