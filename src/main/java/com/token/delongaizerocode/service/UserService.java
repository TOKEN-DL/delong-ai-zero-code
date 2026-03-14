package com.token.delongaizerocode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.token.delongaizerocode.entity.User;
import com.token.delongaizerocode.model.dto.user.UserQueryRequest;
import com.token.delongaizerocode.model.vo.LoginUserVO;
import com.token.delongaizerocode.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import javax.management.Query;
import java.util.List;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/TOKEN-DL">时雨夏树</a>
 */
public interface UserService extends IService<User> {

    /**
     *  用户注册
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 确认密码
     * @return 新用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    String getEncryptPassword(String userPassword);

    /**
     *  获取脱敏用户的登录信息
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);


    /**
     *  获取脱敏后的用户信息
     * @param user 用户
     * @return
     */
    UserVO getUserVO(User user);



    /**
     *  获取脱敏后的用户信息列表
     * @param userList 用户列表
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);


    /**
     * 根据查询条件构造数据查询参数
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);





    /**
     * 用户登录
     *
     * @param userAccount 账号
     * @param userPassword 密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

}
