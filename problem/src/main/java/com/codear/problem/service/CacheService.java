package com.codear.problem.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;

    public void setValue(String key, String value){
        redisTemplate.opsForValue().set(key, value, 10, TimeUnit.MINUTES);
    }

    public String getValue(String key){
        return redisTemplate.opsForValue().get(key);
    }
}