package com.rideshare.locationservice.config;

//crete the bean of rest template make the data human readable redis cli and return it

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;


@Configuration
public class RedisConfig {
    public RedisTemplate<String, String> redisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        //string serializer make the data human readable
        template.setKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
        template.setValueSerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
        return template;
    }
}
