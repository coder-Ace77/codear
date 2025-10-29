package com.codear.problem.service;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserServiceJWT {

    private final JwtService jwtService;

    public Long getUserIdByToken(String authHeader){
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String jwt = authHeader.substring(7);
        return jwtService.extractUserId(jwt.strip());
    }    
}
