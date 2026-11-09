package com.codear.engine.checker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.codear.engine.dto.ResourceConstraints;

class ComparatorsTest {

    @Nested
    class Tokens {
        private final OutputComparator comparator = new TokenComparator();

        @Test
        void identicalOutputMatches() {
            assertTrue(comparator.matches("20", "20"));
        }

        @Test
        void spacingTrailingBlanksAndBlankLinesAreIgnored() {
            assertTrue(comparator.matches("20", "  20  \n\n"));
            assertTrue(comparator.matches("1\n2\n3", "1 2 3"));
            assertTrue(comparator.matches("YES\nNO\n", "YES\r\nNO\r\n\r\n"));
        }

        @Test
        void aDifferentWordDoesNotMatch() {
            assertFalse(comparator.matches("20", "21"));
        }

        @Test
        void aMissingOrExtraWordDoesNotMatch() {
            assertFalse(comparator.matches("1 2", "1"));
            assertFalse(comparator.matches("1", "1 2"));
        }

        @Test
        void caseMatters() {
            assertFalse(comparator.matches("YES", "yes"));
        }

        @Test
        void nothingAgainstNothingMatchesAndNothingAgainstSomethingDoesNot() {
            assertTrue(comparator.matches("", "  \n"));
            assertTrue(comparator.matches(null, ""));
            assertFalse(comparator.matches("20", ""));
            assertFalse(comparator.matches("20", null));
        }

        @Test
        void joinedWordsAreNotTheSameAsSeparateOnes() {
            assertFalse(comparator.matches("1 2", "12"));
        }

        @Test
        void aMultiMegabyteOutputIsComparedWithoutTrouble() {
            String big = "YES\n".repeat(2_000_000);
            assertTrue(comparator.matches(big, big.replace("\n", " ")));
        }
    }

    @Nested
    class Floats {
        private final OutputComparator comparator = new FloatComparator(1e-6);

        @Test
        void numbersWithinTheToleranceMatch() {
            assertTrue(comparator.matches("3.141592", "3.1415923"));
            assertTrue(comparator.matches("100000.0", "100000.05"));
        }

        @Test
        void numbersOutsideTheToleranceDoNot() {
            assertFalse(comparator.matches("3.14", "3.15"));
        }

        @Test
        void wordsThatAreNotNumbersMustBeIdentical() {
            assertTrue(comparator.matches("YES 1.0", "YES 1.0000001"));
            assertFalse(comparator.matches("YES 1.0", "NO 1.0"));
        }

        @Test
        void notANumberNeverMatchesANumber() {
            assertFalse(comparator.matches("1.0", "NaN"));
            assertFalse(comparator.matches("1.0", "Infinity"));
        }

        @Test
        void aDifferentNumberOfWordsDoesNotMatch() {
            assertFalse(comparator.matches("1.0 2.0", "1.0"));
        }
    }

    @Nested
    class Create {
        private OutputComparator of(String checker, Double tolerance) {
            return ComparatorFactory.getCheckerFromConstraints(new ResourceConstraints(null, null, null, checker, tolerance));
        }

        @Test
        void floatUsesTheGivenToleranceOrADefault() {
            assertTrue(of("FLOAT", 0.1).matches("1.0", "1.05"));
            assertFalse(of("FLOAT", null).matches("1.0", "1.05"));
            assertFalse(of("float", -1.0).matches("1.0", "1.05"));
        }

        @Test
        void anythingElseIsTokens() {
            assertInstanceOf(TokenComparator.class, ComparatorFactory.getCheckerFromConstraints(null));
            assertInstanceOf(TokenComparator.class, of(null, null));
            assertInstanceOf(TokenComparator.class, of("EXACT_LINES", null));
        }
    }
}
