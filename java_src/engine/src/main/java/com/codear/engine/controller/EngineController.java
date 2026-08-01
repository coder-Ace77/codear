package com.codear.engine.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class EngineController {
    
    @GetMapping("/health")
    public String getMethodName(){
        return new String("Engine service is up and running");
    }
}
