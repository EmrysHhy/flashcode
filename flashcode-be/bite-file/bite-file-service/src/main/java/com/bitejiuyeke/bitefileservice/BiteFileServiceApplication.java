package com.bitejiuyeke.bitefileservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 文件服务启动类
 */
@Slf4j
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, RabbitAutoConfiguration.class})
public class BiteFileServiceApplication {

    /**
     * 启动函数
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BiteFileServiceApplication.class, args);
        log.info("文件服务启动成功!");
    }

}
