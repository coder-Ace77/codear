package com.codear.engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.awspring.cloud.sqs.annotation.SqsListener;

/**
 * Guards the queue settings the library only checks when the application starts. A bad combination there takes the
 * whole engine down at boot, and nothing else in the unit tests starts the application to notice.
 */
class QueueListenerSettingsTest {

    private static List<SqsListener> listeners() {
        return Arrays.stream(SqsReceiver.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(SqsListener.class))
                .filter(annotation -> annotation != null)
                .toList();
    }

    private static int number(String value, int whenUnset) {
        return value == null || value.isBlank() ? whenUnset : Integer.parseInt(value.trim());
    }

    @Test
    void thereAreTwoListeners() {
        assertEquals(2, listeners().size());
    }

    @Test
    void aPollNeverFetchesMoreMessagesThanCanRunAtOnce() {
        for (SqsListener listener : listeners()) {
            // the library's defaults when a setting is left out: 10 concurrent, 10 per poll
            int concurrent = number(listener.maxConcurrentMessages(), 10);
            int perPoll = number(listener.maxMessagesPerPoll(), 10);

            assertTrue(perPoll <= concurrent,
                    listener.value()[0] + ": " + perPoll + " messages per poll but only " + concurrent + " at once");
        }
    }

    @Test
    void everyListenerSetsBothSoNeitherFallsBackToALibraryDefault() {
        for (SqsListener listener : listeners()) {
            assertFalse(listener.maxConcurrentMessages().isBlank(), listener.value()[0] + " leaves concurrency unset");
            assertFalse(listener.maxMessagesPerPoll().isBlank(), listener.value()[0] + " leaves messages-per-poll unset");
        }
    }

    @Test
    void aSubmissionIsNotHandedToASecondWorkerWhileItIsBeingJudged() {
        for (SqsListener listener : listeners()) {
            assertTrue(number(listener.messageVisibilitySeconds(), 0) >= 120,
                    listener.value()[0] + ": visibility timeout is shorter than a long judging run");
        }
    }
}
