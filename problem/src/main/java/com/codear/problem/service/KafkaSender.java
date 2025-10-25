package com.codear.problem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class KafkaSender {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final String TOPIC = "code-submit";

    public void sendMessage(Object message){
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(TOPIC, jsonMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }    
}
