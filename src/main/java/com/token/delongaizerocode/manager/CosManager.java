package com.token.delongaizerocode.manager;


import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.token.delongaizerocode.config.CosClientConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.UUID;

/**
 * COS对象存储管理器
 */
@Component
@ConditionalOnBean(COSClient.class)
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     *
     * @param key  唯一键
     * @param file 文件
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file); // 主要COS上传方法
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传文件COS并返回到URL
     *
     * @param key COS对象键（完整路径）
     * @param file 要上传德文件
     * @return 文件的访问URL，失败返回null
     */
    public String uploadFile(String key, File file){
        PutObjectResult result = putObject(key, file);
        if (result != null) {
            String url = String.format("%s/%s", cosClientConfig.getHost(), key);
            log.info("文件上传到COS成功:{} -> {}",file.getName(), url);
            return url;
        }else {
            log.info("文件上传到COS失败： {}，返回结果为空",file.getName());
            return null;
        }
    }








}
