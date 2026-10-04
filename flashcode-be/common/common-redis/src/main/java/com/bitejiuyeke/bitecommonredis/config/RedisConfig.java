package com.bitejiuyeke.bitecommonredis.config;

import com.bitejiuyeke.bitecommondomain.constants.CommonConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * redis配置
 */
@Configuration
public class RedisConfig {

    /**
     * redis序列化
     * @param redisConnectionFactory redis连接工厂
     * @return redis操作模板
     */
    //完成对于redisTemplate 的配置
    //修饰符  返回值类型 函数名  参数列表  函数体
    //key  value
    //key  value (hashKey  hashValue)
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //Redis 中key的序列化设置
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        //Redis 中value的序列化设置
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = createJacksonSerializer();
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 创建序列化器
     * @return redis序列化器
     */
    private GenericJackson2JsonRedisSerializer createJacksonSerializer() {
        ObjectMapper OBJECT_MAPPER = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
                .configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, false)
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .addModule(new JavaTimeModule())
                .addModule(new SimpleModule()
                        .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(CommonConstants.STANDARD_FORMAT)))
                        .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(CommonConstants.STANDARD_FORMAT)))
                )
                .defaultDateFormat(new SimpleDateFormat(CommonConstants.STANDARD_FORMAT))
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
        return new GenericJackson2JsonRedisSerializer(OBJECT_MAPPER);
    }
}
