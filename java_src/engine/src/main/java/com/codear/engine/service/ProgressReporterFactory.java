package com.codear.engine.service;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class ProgressReporterFactory {

    private final CacheService cacheService;
    private final ObjectMapper mapper;

    /** The problem service has already written version 1 (QUEUED), so the engine continues from 2. */
    public ProgressReporter create(String submissionId) {
        return new ProgressReporter(cacheService, mapper, submissionId, 2, System::currentTimeMillis);
    }
}
