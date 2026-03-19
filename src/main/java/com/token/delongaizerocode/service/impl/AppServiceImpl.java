package com.token.delongaizerocode.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.token.delongaizerocode.model.entity.App;
import com.token.delongaizerocode.exception.BusinessException;
import com.token.delongaizerocode.exception.ErrorCode;
import com.token.delongaizerocode.mapper.AppMapper;
import com.token.delongaizerocode.model.dto.app.AppQueryRequest;
import com.token.delongaizerocode.model.entity.User;
import com.token.delongaizerocode.model.vo.AppVO;
import com.token.delongaizerocode.model.vo.UserVO;
import com.token.delongaizerocode.service.AppService;
import com.token.delongaizerocode.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

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
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

    @Resource
    private UserService userService;

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
