package com.codear.engine.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List; 
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper; 

    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }
    
    public void set(String key, String value) {
        set(key, value, 500, TimeUnit.MINUTES);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, timeout, unit);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize object for cache: " + e.getMessage());
        }
    }
    
    public void set(String key, Object value) {
        set(key, value, 500, TimeUnit.MINUTES);
    }


    public <T> T get(String key, Class<T> clazz) {
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

    public <T> List<T> getList(String key, Class<T> elementClass) {
        String jsonValue = redisTemplate.opsForValue().get(key);
        if (jsonValue == null) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonValue,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass));
        } catch (IOException e) {
            return null;
        }
    }
    
    public void del(String key) {
        redisTemplate.delete(key);
    }
}