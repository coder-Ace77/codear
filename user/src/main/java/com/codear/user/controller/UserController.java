package com.codear.user.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class UserController {
    
    @GetMapping("/health")
    public String getMethodName(){
        return new String("User service is up and running");
    }
    
}
