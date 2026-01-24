package com.codear.engine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "test_cases", indexes = @Index(name = "idx_testcase_problem_id", columnList = "problemId"))
public class TestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "TEXT")
    private String input;
    @Column(columnDefinition = "TEXT")
    private String output;
    private boolean isSample;
    private Boolean isHidden = true;
    private Long problemId;
}