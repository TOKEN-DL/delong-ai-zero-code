package com.token.delongaizerocode.model.vo;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserVO implements Serializable {


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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;



    public static final long serialVersionUID = 1L;
}
