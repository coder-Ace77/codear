package com.codear.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// Import BOTH classes to exclude
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;

// Add BOTH classes to the 'exclude' property
@SpringBootApplication(exclude = { 
    HttpClientAutoConfiguration.class,
    RestClientAutoConfiguration.class 
})
public class EngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineApplication.class, args);
    }

}