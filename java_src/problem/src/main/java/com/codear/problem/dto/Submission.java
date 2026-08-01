package com.codear.problem.dto;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

import com.codear.problem.enums.SubmissionStatus;

@Entity
@Table(name = "submissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String submissionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long problemId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(nullable = false)
    private String language;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(columnDefinition = "TEXT")
    private String errorLog;

    private Integer totalTests;
    private Integer passedTests;
    private LocalDateTime submittedAt;
    private Long timeTakenMs;
    private String memoryUsed;
}
