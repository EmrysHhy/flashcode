package com.bitejiuyeke.bitecommoncore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate模板配置
 */
@Configuration
public class RestTemplateConfig {


    /**
     * 注册RestTemplate
     *
     * @param factory http工程
     * @return RestTemplate对象
     */
    @Bean
    public RestTemplate restTemplate(ClientHttpRequestFactory factory) {
        return new RestTemplate(factory);
    }

    /**
     * http客户端工厂配置
     *
     * @return http客户端工厂
     */
    @Bean
    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(10000);//ms
        factory.setConnectTimeout(15000);//ms
        return factory;
    }
}
