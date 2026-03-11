package com.token.delongaizerocode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.token.delongaizerocode.mapper")
public class DelongAiZeroCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DelongAiZeroCodeApplication.class, args);
    }

}
