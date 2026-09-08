package com.bitejiuyeke.bitemstemplateservice;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 */
@Slf4j
@MapperScan("com.bitejiuyeke.**.mapper")
@SpringBootApplication
public class BiteMstemplateServiceApplication {

    /**
     * 主函数
     * @param args 入参
     */
    public static void main(String[] args) {
        SpringApplication.run(BiteMstemplateServiceApplication.class, args);
    }
}
