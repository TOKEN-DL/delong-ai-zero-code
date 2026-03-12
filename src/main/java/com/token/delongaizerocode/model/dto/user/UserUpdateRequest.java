package com.token.delongaizerocode.model.dto.user;

import lombok.Data;

@Data
public class UserUpdateRequest {


    /**
     * 用户id
     */
    private Long id;
    /**
     * 用户昵称
     */
    private String userName;


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
