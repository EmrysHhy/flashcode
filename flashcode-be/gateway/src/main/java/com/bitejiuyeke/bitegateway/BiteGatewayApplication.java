package com.bitejiuyeke.bitegateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动类
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@Slf4j
public class BiteGatewayApplication {
    /**
     * 主函数
     * @param args 入参
     */
    public static void main(String[] args) {
        SpringApplication.run(BiteGatewayApplication.class, args);
        log.info("网关启动成功");
    }
}
