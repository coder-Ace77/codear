package com.codear.engine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List; // Import List
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
        setValue(key, value, 10, TimeUnit.MINUTES); // Default to 10 minutes
    }

    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void setObjectValue(String key, Object value, long timeout, TimeUnit unit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, timeout, unit);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize object for cache: " + e.getMessage());
        }
    }
    
    // Helper for setObjectValue with default 10 min expiry
    public void setObjectValue(String key, Object value) {
        setObjectValue(key, value, 10, TimeUnit.MINUTES);
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

    /**
     * NEW METHOD: Deserializes a list of objects from a JSON string.
     */
    public <T> List<T> getObjectListValue(String key, Class<T> elementClass) {
        String jsonValue = redisTemplate.opsForValue().get(key);
        if (jsonValue == null) {
            return null;
        }
        try {
            // Use ObjectMapper's TypeFactory to construct the correct List<T> type
            return objectMapper.readValue(jsonValue,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass));
        } catch (IOException e) {
            System.err.println("Failed to deserialize list from cache: " + e.getMessage());
            return null;
        }
    }
    
    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }
}