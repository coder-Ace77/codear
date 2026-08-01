package com.codear.problem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SqsSender {

    @Autowired
    private SqsTemplate sqsTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final String QUEUE_URL = "https://sqs.ap-south-1.amazonaws.com/829108230523/codear-queue";

    public void sendMessage(Object message){
        try {
            String json = objectMapper.writeValueAsString(message);
            sqsTemplate.send(QUEUE_URL, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Object message, String queueUrl){
        try {
            String json = objectMapper.writeValueAsString(message);
            sqsTemplate.send(queueUrl, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
