package com.codear.problem.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "problems")
@Data
@EqualsAndHashCode(exclude = "testCases") 
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

    @Column(columnDefinition = "text[]")
    private List<String> tags;

    private Long timeLimitMs;

    private Integer memoryLimitMb;

    @OneToMany(
        mappedBy = "problem", 
        cascade = CascadeType.ALL,
        orphanRemoval = true, 
        fetch = FetchType.LAZY 
    )
    @JsonManagedReference
    private List<TestCase> testCases;
    
}