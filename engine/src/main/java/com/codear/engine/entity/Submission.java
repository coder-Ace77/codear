package com.codear.engine.entity;

import java.time.LocalDateTime; 
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import com.codear.engine.enums.RunStatus; 

import lombok.Data;

@Data
@Entity
@Table(name = "submissions") 
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long problemId;

    @Column(nullable = false, columnDefinition = "TEXT") 
    private String code;

    @Column(nullable = false)
    private String language;

    @Enumerated(EnumType.STRING) 
    private RunStatus status;    

    @Column(columnDefinition = "TEXT") 
    private String result;

    private Integer totalTests;
    
    private Integer passedTests;

    private LocalDateTime submittedAt; 

    private Long timeTakenMs; 

    private String memoryUsed; 
}