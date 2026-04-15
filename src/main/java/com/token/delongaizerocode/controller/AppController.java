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
import com.token.delongaizerocode.model.vo.AppVO;
import com.token.delongaizerocode.ratelimter.annotation.RateLimit;
import com.token.delongaizerocode.ratelimter.enums.RateLimitType;
import com.token.delongaizerocode.service.AppService;
import com.token.delongaizerocode.service.ProjectDownloadService;
import com.token.delongaizerocode.service.UserService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.LocalDateTime;
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

    @Resource
    private ProjectDownloadService projectDownloadService;




    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60, message = "AI请求过于频繁，请稍后再试")
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


    @GetMapping("/download/{appId}")
    public void downloadAppCode(@RequestParam Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response){
        //1.基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");

        //2.查询应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_LOGIN_ERROR, "应用不存在");
        //3.权益校验：只有应用创建者才可以下载代码
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "无权限下载该应用代码");
        }
        //4.构建应用代码目录路径（生成路径，非部署路径）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        //5.检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.PARAMS_ERROR, "应用不存在，请先生成代码");
        //6.生成下载文件名
        String downloadFileName = String.valueOf(appId);
        //7.调用通用业务下载服务
        projectDownloadService.downloadProjectAsZip(sourceDirPath,downloadFileName,response);


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
        User loginUser = userService.getLoginUser(request);
        Long appId = appService.createApp(appAddRequest, loginUser);
        return ResultUtils.success(appId);
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
    @Cacheable(value = "good_app_page",
            key = "T(com.token.delongaizerocode.utils.CacheKeyUtils).generateKey(#appQueryRequest)",
            condition = "#appQueryRequest.pageNum <= 10"
    )
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
