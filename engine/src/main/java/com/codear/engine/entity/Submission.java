package com.codear.engine.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import com.codear.engine.enums.RunStatus;
import lombok.Data;

@Data
@Entity
@Table(name = "submissions", indexes = {
        @Index(name = "idx_submission_user_problem", columnList = "userId, problemId"),
        @Index(name = "idx_submission_user_submitted", columnList = "userId, submittedAt")
})
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
    private RunStatus status;

    @Column(columnDefinition = "TEXT")
    private String result;

    private Integer totalTests;

    private Integer passedTests;

    private LocalDateTime submittedAt;

    private Long timeTakenMs;

    private String memoryUsed;
}
