package com.codear.user.controller;

import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.codear.user.dto.LoginDTO;
import com.codear.user.dto.RegisterDTO;
import com.codear.user.entity.User;
import com.codear.user.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;


import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/health")
    public String getMethodName(){
        return new String("User service is up and running");
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterDTO registerDTO) {
        try {
            User registeredUser = userService.registerUser(registerDTO);
            return ResponseEntity.ok("User registered successfully: " + registeredUser.getUsername());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        System.out.println("Login request"+loginDTO);
        try {
            System.out.println("TRY!!!");
            String token = userService.loginUser(loginDTO);
            System.out.println("DONE"+token);
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user")
    public User getUser(@RequestHeader(name="Authorization" , required = false) String authString){
        return userService.getUserByToken(authString);
    }
    
      
}
