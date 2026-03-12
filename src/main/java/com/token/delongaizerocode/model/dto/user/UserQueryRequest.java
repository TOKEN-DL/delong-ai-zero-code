package com.token.delongaizerocode.model.dto.user;

import com.token.delongaizerocode.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户查询请求
 */
@Data
public class UserQueryRequest extends PageRequest implements Serializable {



    /**
     * 用户id
     */
    private Long id;
    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色： user , admin
     */
    private String userRole;

    public static final long serialVersionUID = 1L;
}
