package com.codear.user.controller;

import com.codear.user.entity.User;
import com.codear.user.service.AiService;
import com.codear.user.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class ChatController {

    private final AiService aiService;
    private final UserService userService;

    @PostMapping("/chat")
    public ResponseEntity<?> chatWithAi(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChatRequest request) {

        User user = userService.getUserByToken(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String reply = aiService.getAiResponse(
                user, 
                request.getProblemStatement(), 
                request.getCode(), 
                request.getUserMessage()
        );

        return ResponseEntity.ok(Map.of("reply", reply));
    }
}

@Data
class ChatRequest {
    private String problemStatement;
    private String code;
    private String userMessage;
}