package com.token.delongaizerocode.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.token.delongaizerocode.constant.AppConstant;
import com.token.delongaizerocode.core.AiCodeGeneratorFacade;
import com.token.delongaizerocode.core.handler.StreamHandlerExecutor;
import com.token.delongaizerocode.exception.ThrowUtils;
import com.token.delongaizerocode.model.entity.App;
import com.token.delongaizerocode.exception.BusinessException;
import com.token.delongaizerocode.exception.ErrorCode;
import com.token.delongaizerocode.mapper.AppMapper;
import com.token.delongaizerocode.model.dto.app.AppQueryRequest;
import com.token.delongaizerocode.model.entity.User;
import com.token.delongaizerocode.model.enums.ChatHistoryMessageTypeEnum;
import com.token.delongaizerocode.model.enums.CodeGenTypeEnum;
import com.token.delongaizerocode.model.vo.AppVO;
import com.token.delongaizerocode.model.vo.UserVO;
import com.token.delongaizerocode.service.AppService;
import com.token.delongaizerocode.service.ChatHistoryService;
import com.token.delongaizerocode.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/TOKEN-DL">时雨夏树</a>
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

    @Resource
    private UserService userService;



    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;


    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        //1.参数校验
        ThrowUtils.throwIf(appId == null || appId <=0 , ErrorCode.PARAMS_ERROR, "应用ID错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");

        //2.查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");

        //3.权限校验，仅本人可以和自己的应用对话
        if (!app.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "无权访问应用");
        }


        //4.获取应用生成代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码生成类型错误 ");

        }

        //5.在调用AI前，先保存用户消息到数据库中
        chatHistoryService.addChatHistory(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());

        //6.调用AI生成代码（流式）
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        //7. 收集AI响应的内容
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum);


        //5.调用AI生成代码

        //return aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);

    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        //1.参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0 , ErrorCode.PARAMS_ERROR, "应用ID错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        //2.查询应用信息

        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        //3.权限校验，仅本人
        if (!app.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "无权访问该应用");
        }

        //4.检查是否已有 deployKey
        //如果没有，则生成6位 deployKey(字母 + 数字)
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)){
            deployKey = RandomUtil.randomString(6);

        }

        //5.获取代码生成类型，获取原始代码生成路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" +appId;
        String sourceFilePath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;

        //6.检查路径是否存在
        File sourceDir = new File(sourceFilePath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成应用");
        }

        //7.复制文件到部署目录

        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败");
        }

        //8.更新数据库
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setUpdateTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");

        //9.返回可以访问的URL
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }


    @Override
    public AppVO getAppVO(App app) {
        if (app == null){
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtils.copyProperties(app, appVO);
        //关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null){
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)){
            return new ArrayList<>();
        }

        //批量获取用户信息，避免N+1查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());

        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));


        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        Boolean featured = appQueryRequest.getFeatured();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));

        // 如果只查询精选应用，则查询优先级不为0的应用
        if (featured != null && featured) {
            queryWrapper.ne("priority", 0);
        }

        return queryWrapper;
    }

    /**
     * 删除应用时,关联删除对话历史
     * @param id
     * @return
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null){
            return false;
        }
        long appId = Long.parseLong(id.toString());
        if (appId <= 0){
            return false;
        }
        //先删除关联的对话历史
        try {
            //删除应用前，先删除消息历史
            chatHistoryService.deleteByAppId(appId);
        }catch (Exception e){
            log.error("删除应用关联的对话历史失败： {}" , e.getMessage());
        }

        //删除应用
        return super.removeById(id);
    }

    @Override
    public void validApp(App app) {
        if (app == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用参数为空");
        }
        String appName = app.getAppName();
        String initPrompt = app.getInitPrompt();

        // 应用名称校验
        if (StrUtil.isBlank(appName)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用名称不能为空");
        }
        if (appName.length() > 80){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用名称过长");
        }

        // 初始化 Prompt 校验
        if (StrUtil.isBlank(initPrompt)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "初始化 Prompt 不能为空");
        }
        if (initPrompt.length() > 5000){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "初始化 Prompt 过长");
        }





    }


}
