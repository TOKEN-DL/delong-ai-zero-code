package com.token.delongaizerocode.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用更新请求（管理员）
 */
@Data
public class AppUpdateByAdminRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 优先级
     */
    private Integer priority;

    public static final long serialVersionUID = 1L;
}