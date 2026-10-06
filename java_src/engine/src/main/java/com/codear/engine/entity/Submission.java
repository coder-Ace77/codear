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

    /** Why it got that status (a Verdict name). A plain string, so adding verdicts never needs a schema change. */
    @Column(length = 40)
    private String verdict;

    /** 1-based number of the first failing test, if any. */
    private Integer failedTest;

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
