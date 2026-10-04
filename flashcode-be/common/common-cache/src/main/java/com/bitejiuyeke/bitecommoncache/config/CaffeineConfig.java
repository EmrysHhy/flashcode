package com.bitejiuyeke.bitecommoncache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置
 */
@Configuration
public class CaffeineConfig {

    /**
     * 初始容量
     */
    @Value("${caffeine.build.initial-capacity}")
    private Integer initialCapacity;

    /**
     * 最大容量
     */
    @Value("${caffeine.build.maximum-size}")
    private Long maximumSize;

    /**
     * 过期时间
     */
    @Value("${caffeine.build.expire}")
    private Long expire;


    /**
     * 构造本地缓存对象
     *
     * @return 本地缓存对象
     */
    @Bean
    public Cache<String, Object> caffeineCache() {
        return Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .maximumSize(maximumSize)
                .expireAfterWrite(expire, TimeUnit.SECONDS)
                .build();
    }
}
