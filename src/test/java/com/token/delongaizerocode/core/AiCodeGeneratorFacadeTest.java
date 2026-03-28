package com.token.delongaizerocode.core;

import com.token.delongaizerocode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
//        File file = aiCodeGeneratorFacade.generateAndSaveCode("生成一个登录页面,总共不超过20行代码", CodeGenTypeEnum.MULTI_FILE);
//        Assertions.assertNotNull(file);
    }

    @Test
    void testGenerateAndSaveCode() {
        Flux<String> codeStream = aiCodeGeneratorFacade
                .generateAndSaveCodeStream("生成一个博客网站,总共不超过20行代码",
                CodeGenTypeEnum.VUE_PROJECT,1L);
        //等待所有数据搜集完成
        List<String> result = codeStream.collectList().block();
        // 验证结果
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

    @Test
    void generateAndSaveCodeStream() {
    }
}