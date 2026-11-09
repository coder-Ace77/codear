package com.codear.engine.entity;



import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Data;

@Entity
@Table(name = "problems")
@Data
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String inputDescription;
    @Column(columnDefinition = "TEXT")
    private String outputDescription;
    @Column(columnDefinition = "TEXT")
    private String constraints;
    private String difficulty;
    
    @Column(columnDefinition = "text[]")
    private List<String> tags;

    private Integer memoryLimitMb;
    private Long timeLimitMs;

    private String checker;
    /** For the FLOAT checker: how far apart two numbers may be. */
    private Double checkerTolerance;
}