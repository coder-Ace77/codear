package com.codear.engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TestProgressParserTest {

    private static final String SEP = ContainerFactory.OUTPUT_SEPARATOR;

    /** Records every event as text, in order. */
    private static class Recorder implements TestProgressListener {
        final List<String> events = new ArrayList<>();

        @Override
        public void testStarted(int number) {
            events.add("start " + number);
        }

        @Override
        public void testFinished(int number) {
            events.add("finish " + number);
        }
    }

    private static String oneTest(int index, String output) {
        return "[TEST-START-" + index + "]\n[TEST-OUTPUT-START]\n" + output + "\n\n[TEST-OUTPUT-END]\n" + SEP + "\n";
    }

    @Test
    void reportsEachTestStartAndFinishInOrder() {
        Recorder recorder = new Recorder();
        TestProgressParser parser = new TestProgressParser(recorder);

        parser.feed(oneTest(0, "YES") + oneTest(1, "NO") + oneTest(2, "YES"));

        assertEquals(List.of("start 1", "finish 1", "start 2", "finish 2", "start 3", "finish 3"), recorder.events);
    }

    @Test
    void handlesOutputSplitAtAnyByte() {
        String whole = oneTest(0, "YES") + oneTest(1, "NO");
        for (int cut = 1; cut < whole.length(); cut += 7) {
            Recorder recorder = new Recorder();
            TestProgressParser parser = new TestProgressParser(recorder);
            parser.feed(whole.substring(0, cut));
            parser.feed(whole.substring(cut));
            assertEquals(List.of("start 1", "finish 1", "start 2", "finish 2"), recorder.events, "cut at " + cut);
        }
    }

    @Test
    void worksOneCharacterAtATime() {
        Recorder recorder = new Recorder();
        TestProgressParser parser = new TestProgressParser(recorder);
        for (char c : (oneTest(0, "7") + oneTest(1, "8")).toCharArray()) {
            parser.feed(String.valueOf(c));
        }
        assertEquals(List.of("start 1", "finish 1", "start 2", "finish 2"), recorder.events);
    }

    @Test
    void reportsAStartedTestThatNeverFinishes() {
        Recorder recorder = new Recorder();
        TestProgressParser parser = new TestProgressParser(recorder);

        parser.feed(oneTest(0, "ok") + "[TEST-START-1]\n[TEST-OUTPUT-START]\n");

        assertEquals(List.of("start 1", "finish 1", "start 2"), recorder.events);
    }

    @Test
    void ignoresCarriageReturnsAndSurroundingSpaces() {
        Recorder recorder = new Recorder();
        new TestProgressParser(recorder).feed("  [TEST-START-0]  \r\n" + SEP + "\r\n");
        assertEquals(List.of("start 1", "finish 1"), recorder.events);
    }

    @Test
    void ignoresProgramOutputThatIsNotAMarker() {
        Recorder recorder = new Recorder();
        TestProgressParser parser = new TestProgressParser(recorder);

        parser.feed("hello\n[TEST-START-abc]\n[TEST-START-]\nsome " + SEP + " inside a line\nMETRICS:0.01:3400\n");

        assertEquals(List.of(), recorder.events);
    }

    @Test
    void aHugeLineWithoutANewlineDoesNotGrowMemoryOrBreakLaterMarkers() {
        Recorder recorder = new Recorder();
        TestProgressParser parser = new TestProgressParser(recorder);

        parser.feed("x".repeat(5_000_000));
        parser.feed("\n" + oneTest(0, "after"));

        assertEquals(List.of("start 1", "finish 1"), recorder.events);
    }

    @Test
    void emptyAndNullChunksAreHarmless() {
        Recorder recorder = new Recorder();
        TestProgressParser parser = new TestProgressParser(recorder);
        parser.feed(null);
        parser.feed("");
        assertEquals(List.of(), recorder.events);
    }
}
