package com.token.delongaizerocode;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;


@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.token.delongaizerocode.mapper")
public class DelongAiZeroCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DelongAiZeroCodeApplication.class, args);
    }

}
