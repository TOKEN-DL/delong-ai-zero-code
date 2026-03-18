package com.token.delongaizerocode.controller;

import com.mybatisflex.core.paginate.Page;
import com.token.delongaizerocode.annotation.AuthnCheck;
import com.token.delongaizerocode.common.BaseResponse;
import com.token.delongaizerocode.common.DeleteRequest;
import com.token.delongaizerocode.common.ResultUtils;
import com.token.delongaizerocode.constant.UserConstant;
import com.token.delongaizerocode.exception.BusinessException;
import com.token.delongaizerocode.exception.ErrorCode;
import com.token.delongaizerocode.exception.ThrowUtils;
import com.token.delongaizerocode.model.dto.app.*;
import com.token.delongaizerocode.model.entity.App;
import com.token.delongaizerocode.model.entity.User;
import com.token.delongaizerocode.model.vo.AppVO;
import com.token.delongaizerocode.service.AppService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        App app = new App();
        BeanUtils.copyProperties(appAddRequest, app);

        // 获取当前登录用户
        User loginUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        app.setUserId(loginUser.getId());

        // 校验应用数据
        appService.validApp(app);

        // 设置默认优先级
        if (app.getPriority() == null) {
            app.setPriority(0);
        }

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
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 根据id获取应用（管理员）
     * @param id
     * @return
     */
    @GetMapping("/get/admin")
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
    @PostMapping("/delete/admin")
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
        BeanUtils.copyProperties(appUpdateRequest, updateApp);
        updateApp.setId(appUpdateRequest.getId());

        boolean result = appService.updateById(updateApp);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新任意应用（管理员）
     * @param appUpdateByAdminRequest
     * @return
     */
    @PostMapping("/update/admin")
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
        appQueryRequest.setFeatured(true);

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
    @PostMapping("/list/page/admin")
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
