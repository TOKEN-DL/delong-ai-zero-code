package com.token.delongaizerocode.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class LoginUserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 昵称
     */
    private String userName;

    /**
     * 头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色
     */
    private String userRole;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
