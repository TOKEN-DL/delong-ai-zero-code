package com.token.delongaizerocode.service;

/**
 * 截图服务
 */
public interface ScreenshotService {

    /**
     * 通用的截图服务
     *
     * @param webUrl 网址
     * @return
     */
    String generateAndUploadScreenshot(String webUrl);
}
