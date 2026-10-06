package com.bitejiuyeke.imagemcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@Slf4j
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, RabbitAutoConfiguration.class})
public class ImageMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageMcpApplication.class, args);
        log.info("image-mcp 启动成功");
    }
}
