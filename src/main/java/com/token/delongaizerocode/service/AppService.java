package com.token.delongaizerocode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.token.delongaizerocode.model.entity.App;
import com.token.delongaizerocode.model.dto.app.AppQueryRequest;
import com.token.delongaizerocode.model.vo.AppVO;

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

}
