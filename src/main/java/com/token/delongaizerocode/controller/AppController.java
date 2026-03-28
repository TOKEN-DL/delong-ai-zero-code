package com.token.delongaizerocode.controller;

import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.token.delongaizerocode.annotation.AuthnCheck;
import com.token.delongaizerocode.common.BaseResponse;
import com.token.delongaizerocode.common.DeleteRequest;
import com.token.delongaizerocode.common.ResultUtils;
import com.token.delongaizerocode.constant.AppConstant;
import com.token.delongaizerocode.constant.UserConstant;
import com.token.delongaizerocode.exception.BusinessException;
import com.token.delongaizerocode.exception.ErrorCode;
import com.token.delongaizerocode.exception.ThrowUtils;
import com.token.delongaizerocode.model.dto.app.*;
import com.token.delongaizerocode.model.entity.App;
import com.token.delongaizerocode.model.entity.User;
import com.token.delongaizerocode.model.enums.CodeGenTypeEnum;
import com.token.delongaizerocode.model.vo.AppVO;
import com.token.delongaizerocode.service.AppService;
import com.token.delongaizerocode.service.UserService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  控制层。
 *
 * @author <a href="https://github.com/TOKEN-DL">时雨夏树</a>
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;


    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                     @RequestParam String message,
                                                        HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        ThrowUtils.throwIf(StringUtils.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null,ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        //调用服务生成代码
        Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser);
        return contentFlux
                .map(chunk -> {
                    //将内容包装成JSON对象
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        //发送结束时间
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));

    }


    /**
     *  应用部署
     * @param appDeployRequest 部署请求
     * @param request  请求
     * @return 部署URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request){
        // 检查部署请求是否为空
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        //获取应用ID
        Long appId = appDeployRequest.getAppId();

        //检查应用ID是否为空
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");

        //获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        //调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);

        //返回部署URL
        return ResultUtils.success(deployUrl);
    }



    /**
     * 创建应用（用户）
     * @param appAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthnCheck
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        if (appAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StringUtils.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化prompt不能为空");
        App app = new App();
        BeanUtils.copyProperties(appAddRequest, app);

        // 获取当前登录用户
        User loginUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        app.setUserId(loginUser.getId());

        // 校验应用数据
        //appService.validApp(app);

        // 设置默认优先级
        if (app.getPriority() == null) {
            app.setPriority(0);
        }
        //应用名称展示为前12位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));

        //暂时设置位多文件生成
        //占时设置成为VUE工程生成
        app.setCodeGenType(CodeGenTypeEnum.VUE_PROJECT.getValue());
        //插入数据库
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(app.getId());
    }

    /**
     * 根据id获取应用（用户）
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthnCheck
    public BaseResponse<AppVO> getAppById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app)); // 最后做了脱敏
    }


    /**
     * 根据id获取应用（管理员）
     * @param id
     * @return
     */
    @GetMapping("/admin/get")
    @AuthnCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<App> getAppByIdAdmin(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(app);
    }

    /**
     * 删除自己的应用（用户）
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthnCheck
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        App app = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);

        // 获取当前登录用户
        User loginUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 只能删除自己的应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }

        boolean result = appService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 删除任意应用（管理员）
     * @param deleteRequest
     * @return
     */
    @PostMapping("/admin/delete")
    @AuthnCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = appService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新自己的应用（用户）
     * @param appUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthnCheck
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        App app = appService.getById(appUpdateRequest.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);

        // 获取当前登录用户
        User loginUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 只能更新自己的应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
        }

        App updateApp = new App();
//        BeanUtils.copyProperties(appUpdateRequest, updateApp);
//        updateApp.setId(appUpdateRequest.getId());

        updateApp.setId(appUpdateRequest.getId());
        updateApp.setAppName(appUpdateRequest.getAppName());
        //设置编辑时间
        updateApp.setEditTime(LocalDateTime.now());

        boolean result = appService.updateById(updateApp);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新任意应用（管理员）
     * @param appUpdateByAdminRequest
     * @return
     */
    @PostMapping("/admin/update")
    @AuthnCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppAdmin(@RequestBody AppUpdateByAdminRequest appUpdateByAdminRequest) {
        if (appUpdateByAdminRequest == null || appUpdateByAdminRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        App app = appService.getById(appUpdateByAdminRequest.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);

        App updateApp = new App();
        BeanUtils.copyProperties(appUpdateByAdminRequest, updateApp);
        updateApp.setId(appUpdateByAdminRequest.getId());

        boolean result = appService.updateById(updateApp);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取自己的应用列表（用户）
     * @param appQueryRequest 查询请求参数
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    @AuthnCheck
    public BaseResponse<Page<AppVO>> listAppByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户
        User loginUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 只查询当前用户的应用
        appQueryRequest.setUserId(loginUser.getId());

        int pageNum = appQueryRequest.getPageNum();
        int pageSize = appQueryRequest.getPageSize();

        // 限制每页最多 20 个
        if (pageSize > 20) {
            pageSize = 20;
        }

        Page<App> appPage = appService.page(Page.of(pageNum, pageSize),
                appService.getQueryWrapper(appQueryRequest));

        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);

        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页获取精选应用列表（用户）
     * @param appQueryRequest 查询请求参数
     * @return
     */
    @PostMapping("/list/featured")
    public BaseResponse<Page<AppVO>> listFeaturedAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 只查询精选应用（优先级不为0）
        appQueryRequest.setFeatured(true);    //设置精选值
        //appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);

        int pageNum = appQueryRequest.getPageNum();
        int pageSize = appQueryRequest.getPageSize();

        // 限制每页最多 20 个
        if (pageSize > 20) {
            pageSize = 20;
        }

        Page<App> appPage = appService.page(Page.of(pageNum, pageSize),
                appService.getQueryWrapper(appQueryRequest));

        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);

        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页获取应用列表（管理员）
     * @param appQueryRequest 查询请求参数
     * @return
     */
    @PostMapping("/admin/list/page")
    @AuthnCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppByPageAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        int pageNum = appQueryRequest.getPageNum();
        int pageSize = appQueryRequest.getPageSize();

        Page<App> appPage = appService.page(Page.of(pageNum, pageSize),
                appService.getQueryWrapper(appQueryRequest));

        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);

        return ResultUtils.success(appVOPage);
    }

}
