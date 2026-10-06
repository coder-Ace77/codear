package com.codear.engine.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Publishes the live progress of one submission to Redis, where the problem service long-polls it.
 *
 * Record (JSON, key progress:&lt;submissionId&gt;): {v, stage, total, started, completed, ts}. `v` only increases;
 * the problem service writes v=1 (QUEUED) when it accepts the submission, so this starts at 2.
 *
 * Progress is cosmetic: every failure here is swallowed so it can never affect judging, and per-test updates
 * are throttled so a fast run does not become a burst of Redis commands. Stage changes are never dropped.
 */
@Slf4j
public class ProgressReporter implements TestProgressListener {

    static final long TTL_MINUTES = 15;
    static final long MIN_TEST_UPDATE_MS = 200;

    private final CacheService cache;
    private final ObjectMapper mapper;
    private final String key;
    private final LongSupplier clockMs;

    private long version;
    private String stage = "PREPARING";
    private Integer total = null;
    private int started = 0;
    private int completed = 0;
    private long lastWriteMs = 0;

    public ProgressReporter(CacheService cache, ObjectMapper mapper, String submissionId, long firstVersion,
            LongSupplier clockMs) {
        this.cache = cache;
        this.mapper = mapper;
        this.key = "progress:" + submissionId;
        this.version = firstVersion - 1;
        this.clockMs = clockMs;
    }

    /** The engine picked the submission up. */
    public void preparing() {
        stage = "PREPARING";
        publish(true);
    }

    /** The tests are loaded, so the bar can show "of N". */
    public void totalKnown(int totalTests) {
        total = totalTests;
        publish(true);
    }

    @Override
    public void containerStarted() {
        stage = "RUNNING";
        started = 0;
        completed = 0;
        publish(true);
    }

    @Override
    public void testStarted(int number) {
        stage = "RUNNING";
        started = Math.max(started, number);
        publish(false);
    }

    @Override
    public void testFinished(int number) {
        stage = "RUNNING";
        completed = Math.max(completed, number);
        publish(false);
    }

    /** All tests ran; outputs are being compared. */
    public void judging() {
        stage = "JUDGING";
        if (total != null) {
            started = total;
            completed = total;
        }
        publish(true);
    }

    /** Call only after the verdict is saved: clients then read it from the database. */
    public void done() {
        stage = "DONE";
        publish(true);
    }

    public void error() {
        stage = "ERROR";
        publish(true);
    }

    private void publish(boolean force) {
        long now = clockMs.getAsLong();
        if (!force && now - lastWriteMs < MIN_TEST_UPDATE_MS) {
            return;
        }
        try {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("v", version + 1);
            record.put("stage", stage);
            record.put("total", total);
            record.put("started", started);
            record.put("completed", completed);
            record.put("ts", now / 1000.0);
            cache.setValue(key, mapper.writeValueAsString(record), TTL_MINUTES, TimeUnit.MINUTES);
            version++;
            lastWriteMs = now;
        } catch (Exception e) {
            log.debug("could not publish progress: {}", e.getMessage());
        }
    }
}
