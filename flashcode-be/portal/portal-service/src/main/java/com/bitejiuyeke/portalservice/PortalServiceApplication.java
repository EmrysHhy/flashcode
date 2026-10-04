package com.bitejiuyeke.portalservice;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 
 */
@Slf4j
@MapperScan("com.bitejiuyeke.**.mapper")
@EnableFeignClients(basePackages = {"com.bitejiuyeke.**.feign"})
@SpringBootApplication
public class PortalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PortalServiceApplication.class, args);
        log.info(" flashcode-service  启动成功");
    }
}