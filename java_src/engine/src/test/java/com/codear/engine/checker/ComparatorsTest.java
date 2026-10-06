package com.codear.engine.checker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ComparatorsTest {

    @Nested
    class Tokens {
        private final OutputComparator comparator = new TokenComparator(false);

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
        void caseMattersByDefault() {
            assertFalse(comparator.matches("YES", "yes"));
            assertFalse(comparator.matches("YES", "Yes"));
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
    class IgnoringCase {
        private final OutputComparator comparator = new TokenComparator(true);

        @Test
        void anyCapitalisationMatches() {
            assertTrue(comparator.matches("YES\nNO", "yEs\nno"));
        }

        @Test
        void differentWordsStillDoNot() {
            assertFalse(comparator.matches("YES", "yep"));
        }
    }

    @Nested
    class Floats {
        private final OutputComparator comparator = new FloatTokenComparator(1e-6);

        @Test
        void numbersWithinTheToleranceMatch() {
            assertTrue(comparator.matches("3.141592", "3.1415923"));
            assertTrue(comparator.matches("100000.0", "100000.05")); // relative: 5e-7 of the value
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
    class ExactLines {
        private final OutputComparator comparator = new ExactLinesComparator();

        @Test
        void trailingSpacesAndBlankLinesAreIgnored() {
            assertTrue(comparator.matches("1\n2", "  1  \n\n2\n\n"));
        }

        @Test
        void lineStructureStillMatters() {
            assertFalse(comparator.matches("1\n2", "1 2"));
        }

        @Test
        void nullIsEmptyAndAnswersCaseMatters() {
            assertTrue(comparator.matches(null, ""));
            assertFalse(comparator.matches("20", null));
            assertFalse(comparator.matches("YES", "yes"));
        }
    }

    @Nested
    class Factory {
        @Test
        void eachModeBuildsItsComparator() {
            assertInstanceOf(TokenComparator.class, ComparatorFactory.create(CheckerMode.TOKENS, null));
            assertInstanceOf(TokenComparator.class, ComparatorFactory.create(CheckerMode.TOKENS_IGNORE_CASE, null));
            assertInstanceOf(ExactLinesComparator.class, ComparatorFactory.create(CheckerMode.EXACT_LINES, null));
            assertInstanceOf(FloatTokenComparator.class, ComparatorFactory.create(CheckerMode.FLOAT, 1e-3));
        }

        @Test
        void theIgnoreCaseModeReallyIgnoresCase() {
            assertTrue(ComparatorFactory.create(CheckerMode.TOKENS_IGNORE_CASE, null).matches("YES", "yes"));
            assertFalse(ComparatorFactory.create(CheckerMode.TOKENS, null).matches("YES", "yes"));
        }

        @Test
        void floatModeUsesTheGivenToleranceOrADefault() {
            assertTrue(ComparatorFactory.create(CheckerMode.FLOAT, 0.1).matches("1.0", "1.05"));
            assertFalse(ComparatorFactory.create(CheckerMode.FLOAT, null).matches("1.0", "1.05"));
            assertFalse(ComparatorFactory.create(CheckerMode.FLOAT, -1.0).matches("1.0", "1.05"));
        }

        @Test
        void theDefaultIsTokensCaseSensitive() {
            assertTrue(ComparatorFactory.defaultComparator().matches("1\n2", "1 2"));
            assertFalse(ComparatorFactory.defaultComparator().matches("YES", "yes"));
        }

        @Test
        void aProblemsStoredSettingsPickTheComparator() {
            assertTrue(ComparatorFactory.forProblem("TOKENS_IGNORE_CASE", null).matches("Yes", "YES"));
            assertTrue(ComparatorFactory.forProblem("FLOAT", 0.01).matches("1.000", "1.004"));
            assertFalse(ComparatorFactory.forProblem("EXACT_LINES", null).matches("1 2", "1\n2"));
            // a problem that never set anything, or an old row with an unrecognised value, gets the default
            assertFalse(ComparatorFactory.forProblem(null, null).matches("YES", "yes"));
            assertTrue(ComparatorFactory.forProblem("legacy-garbage", null).matches("1 2", "1\n2"));
        }

        @Test
        void unknownOrMissingNamesFallBackToTokens() {
            assertEquals(CheckerMode.TOKENS, CheckerMode.fromName(null));
            assertEquals(CheckerMode.TOKENS, CheckerMode.fromName(" "));
            assertEquals(CheckerMode.TOKENS, CheckerMode.fromName("nonsense"));
            assertEquals(CheckerMode.FLOAT, CheckerMode.fromName(" float "));
            assertEquals(CheckerMode.TOKENS_IGNORE_CASE, CheckerMode.fromName("tokens_ignore_case"));
        }
    }
}
