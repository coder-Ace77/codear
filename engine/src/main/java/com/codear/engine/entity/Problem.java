package com.codear.engine.entity;



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
    private String description;
    private String inputDescription;
    private String outputDescription;
    private String constraints;
    private String difficulty;
    private String tags;

    private Integer memoryLimitMb;
    private Long timeLimitMs;
}