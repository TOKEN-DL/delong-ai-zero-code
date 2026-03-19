package com.token.delongaizerocode.model.dto.app;

import com.token.delongaizerocode.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用查询请求
 */
@Data
public class AppQueryRequest extends PageRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;


    /**
     * 封面
     */
    private String cover;

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 代码生成类型（枚举）
     */
    private String codeGenType;

    /**
     * 部署标识
     */
    private String deployKey;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 是否只查询精选应用（优先级不为0）
     */
    private Boolean featured;

    public static final long serialVersionUID = 1L;
}