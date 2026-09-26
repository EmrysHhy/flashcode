package com.bitejiuyeke.biteadminservice.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 声明mq配置信息
 */
@Configuration
@Slf4j
public class RabbitConfig {

    // 交换机的名称
    public final static String EXCHANGE_NAME = "edit_user_exchange";

    /**
     * 广播交换机
     * @return
     */
    @Bean
    public FanoutExchange editUserExchange() {
        return new FanoutExchange(EXCHANGE_NAME, true, true);
    }
}
