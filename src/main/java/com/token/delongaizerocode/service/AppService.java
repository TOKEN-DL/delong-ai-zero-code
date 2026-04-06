package com.token.delongaizerocode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.token.delongaizerocode.model.dto.app.AppAddRequest;
import com.token.delongaizerocode.model.entity.App;
import com.token.delongaizerocode.model.dto.app.AppQueryRequest;
import com.token.delongaizerocode.model.entity.User;
import com.token.delongaizerocode.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/TOKEN-DL">时雨夏树</a>
 */
public interface AppService extends IService<App> {

    /**
     * 获取脱敏后的应用信息
     * @param app 应用
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 获取脱敏后的应用信息列表
     * @param appList 应用列表
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 根据查询条件构造数据查询参数
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 校验应用数据
     * @param app 应用
     */
    void validApp(App app);


    /**
     *
     * 通过对话生成的应用代码
     *
     * @param appId 应用ID
     * @param message 提示词
     * @param loginUser 登录用户
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);


    /**
     * 创建应用
     *
     * @param appAddRequest
     * @param loginUser
     * @return
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 应用部署
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 可访问的部署地址
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId 应用ID
     * @param appDeployUrl 应用访问URL
     */
    void generateAppScreenshotAsync(Long appId, String appDeployUrl);

}
