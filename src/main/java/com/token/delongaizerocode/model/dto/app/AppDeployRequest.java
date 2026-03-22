package com.token.delongaizerocode.model.dto.app;

import lombok.Data;

import java.io.Serializable;


/**
 * 应用部署请求
 */
@Data
public class AppDeployRequest implements Serializable {

    /**
     * 应用ID
     */
    private Long appId;

    private static final long serialVersionUID = 1L;


}
