package com.codear.problem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper; // Spring Boot provides this


    public void setValue(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }
    
    public void setValue(String key, String value) {
        // Default to 10 minutes
        setValue(key, value, 10, TimeUnit.MINUTES);
    }

    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void setObjectValue(String key, Object value, long timeout, TimeUnit unit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, timeout, unit);
        } catch (JsonProcessingException e) {
            // Log this error
            System.err.println("Failed to serialize object for cache: " + e.getMessage());
        }
    }

    public <T> T getObjectValue(String key, Class<T> clazz) {
        String jsonValue = redisTemplate.opsForValue().get(key);
        if (jsonValue == null) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonValue, clazz);
        } catch (IOException e) {
            System.err.println("Failed to deserialize object from cache: " + e.getMessage());
            return null;
        }
    }
    
    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }
}