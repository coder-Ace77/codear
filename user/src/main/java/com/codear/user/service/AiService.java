package com.codear.user.service;

import com.codear.user.entity.ChatMessage;
import com.codear.user.entity.User;
import com.codear.user.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatRepository chatRepository;
    
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    public String getAiResponse(User user, String problemStatement, String code, String userMessage) {
        String systemPrompt = String.format(
            "You are an expert coding assistant. Context:\nProblem: %s\nUser's Current Code: %s\n" +
            "Help the user debug or understand the problem. Keep answers concise.",
            problemStatement, code
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("messages", Arrays.asList(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userMessage)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey); 

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            var response = restTemplate.postForObject(apiUrl, entity, Map.class);

            if (response == null || !response.containsKey("choices")) {
                return "Error: Received empty response from OpenAI.";
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            String aiReply = (String) message.get("content");

            saveMessage(user, userMessage, "user");
            saveMessage(user, aiReply, "assistant");

            return aiReply;

        } catch (Exception e) {
            System.err.println("AI Service Error: " + e.getMessage());
            return "Error calling AI: " + e.getLocalizedMessage();
        }
    }

    private void saveMessage(User user, String content, String role) {
        ChatMessage msg = ChatMessage.builder()
                .user(user)
                .content(content)
                .role(role)
                .timestamp(LocalDateTime.now())
                .build();
        chatRepository.save(msg);
    }
}