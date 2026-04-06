package com.token.delongaizerocode.ai;

import com.token.delongaizerocode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Slf4j
class AiCodeGenTypeRoutingServiceTest {

    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Test
    void routeCodeGenType() {
        String userPrompt = "做一个简单的个人介绍的页面";
        CodeGenTypeEnum codeGenTypeEnum = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        log.info("用户需求：{} - > {}", userPrompt, codeGenTypeEnum);
        userPrompt = "做一个公司官网，需要首页、关于我们、联系我们三个页面";
        codeGenTypeEnum = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        log.info("用户需求：{} - > {}", userPrompt, codeGenTypeEnum);
        userPrompt = "做一个电商管理系统，包含用户管理、商品管理、订单管理，需要路由和状态管理";
        codeGenTypeEnum = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        log.info("用户需求：{} - > {}", userPrompt, codeGenTypeEnum);


    }
}