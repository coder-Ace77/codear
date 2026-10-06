package com.codear.engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ProgressReporterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private CacheService cache;
    private AtomicLong clock;
    private ProgressReporter reporter;

    @BeforeEach
    void setUp() {
        cache = mock(CacheService.class);
        clock = new AtomicLong(1_000_000);
        reporter = new ProgressReporter(cache, mapper, "sub-1", 2, clock::get);
    }

    private List<JsonNode> published() throws Exception {
        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(cache, org.mockito.Mockito.atLeast(0)).setValue(eq("progress:sub-1"), value.capture(), anyLong(),
                eq(TimeUnit.MINUTES));
        List<JsonNode> out = new ArrayList<>();
        for (String json : value.getAllValues()) {
            out.add(mapper.readTree(json));
        }
        return out;
    }

    @Test
    void continuesFromVersionTwoAndOnlyEverIncreases() throws Exception {
        reporter.preparing();
        reporter.totalKnown(3);
        reporter.containerStarted();
        clock.addAndGet(500);
        reporter.testStarted(1);
        clock.addAndGet(500);
        reporter.testFinished(1);
        reporter.judging();
        reporter.done();

        List<JsonNode> records = published();
        assertEquals(2, records.get(0).get("v").asInt());
        for (int i = 1; i < records.size(); i++) {
            assertTrue(records.get(i).get("v").asInt() > records.get(i - 1).get("v").asInt());
        }
    }

    @Test
    void walksThroughTheStagesAndCarriesTheTestCounts() throws Exception {
        reporter.preparing();
        reporter.totalKnown(26);
        reporter.containerStarted();
        clock.addAndGet(300);
        reporter.testStarted(7);
        clock.addAndGet(300);
        reporter.testFinished(6);
        reporter.judging();
        reporter.done();

        List<JsonNode> r = published();
        assertEquals("PREPARING", r.get(0).get("stage").asText());
        assertTrue(r.get(0).get("total").isNull());
        assertEquals(26, r.get(1).get("total").asInt());
        assertEquals("RUNNING", r.get(2).get("stage").asText());
        assertEquals(0, r.get(2).get("started").asInt());

        JsonNode running = r.get(3);
        assertEquals("RUNNING", running.get("stage").asText());
        assertEquals(7, running.get("started").asInt());
        assertEquals(26, running.get("total").asInt());

        JsonNode judging = r.get(r.size() - 2);
        assertEquals("JUDGING", judging.get("stage").asText());
        assertEquals(26, judging.get("completed").asInt());
        assertEquals("DONE", r.get(r.size() - 1).get("stage").asText());
    }

    @Test
    void fastTestsAreThrottledButStagesAreNeverDropped() throws Exception {
        reporter.preparing();
        reporter.totalKnown(100);
        reporter.containerStarted();
        for (int i = 1; i <= 100; i++) {          // 100 tests inside the same 200 ms window
            reporter.testStarted(i);
            reporter.testFinished(i);
        }
        reporter.judging();
        reporter.done();

        List<JsonNode> r = published();
        assertTrue(r.size() <= 7, "expected a handful of writes, got " + r.size());
        assertEquals("JUDGING", r.get(r.size() - 2).get("stage").asText());
        assertEquals("DONE", r.get(r.size() - 1).get("stage").asText());
    }

    @Test
    void aSlowTestIsReportedAsSoonAsTheWindowHasPassed() throws Exception {
        reporter.preparing();
        reporter.totalKnown(5);
        reporter.containerStarted();
        for (int i = 1; i <= 5; i++) {
            clock.addAndGet(1000);
            reporter.testStarted(i);
            clock.addAndGet(1000);
            reporter.testFinished(i);
        }

        List<JsonNode> r = published();
        assertEquals(3 + 10, r.size());
        assertEquals(5, r.get(r.size() - 1).get("completed").asInt());
    }

    @Test
    void errorIsPublishedAsATerminalStage() throws Exception {
        reporter.preparing();
        reporter.error();

        List<JsonNode> r = published();
        assertEquals("ERROR", r.get(r.size() - 1).get("stage").asText());
    }

    @Test
    void aRedisFailureNeverEscapesAndDoesNotBurnAVersion() throws Exception {
        doThrow(new RuntimeException("redis down")).when(cache).setValue(anyString(), anyString(), anyLong(),
                eq(TimeUnit.MINUTES));

        reporter.preparing();
        reporter.totalKnown(3);
        reporter.judging();                         // must not throw

        org.mockito.Mockito.reset(cache);
        reporter.done();

        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(cache, times(1)).setValue(eq("progress:sub-1"), value.capture(), anyLong(), eq(TimeUnit.MINUTES));
        assertEquals(2, mapper.readTree(value.getValue()).get("v").asInt());
    }

    @Test
    void recordsExpireSoTheyDoNotAccumulateInRedis() {
        reporter.preparing();

        verify(cache).setValue(eq("progress:sub-1"), anyString(), eq(15L), eq(TimeUnit.MINUTES));
    }
}
