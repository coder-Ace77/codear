package com.codear.engine.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regression guard. A Spring Data @Modifying query method runs without a transaction unless it is annotated, and
 * Hibernate then throws "Executing an update/delete query": the engine could not save any verdict.
 */
class SubmissionRepositoryTransactionTest {

    @Test
    void everyModifyingQueryRunsInATransaction() {
        for (Method method : SubmissionRepository.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Modifying.class)) {
                assertNotNull(method.getAnnotation(Transactional.class),
                        method.getName() + " is @Modifying but not @Transactional");
            }
        }
    }
}
