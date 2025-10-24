package com.codear.problem.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class ProblemController {
    
    @GetMapping("/health")
    public String getMethodName(){
        return new String("Problem service is up and running");
    }
    
}
