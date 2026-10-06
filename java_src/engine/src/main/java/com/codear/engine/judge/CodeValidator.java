package com.codear.engine.judge;

import static com.codear.engine.constants.JudgeDefaults.MAX_SOURCE_CHARS;

import java.util.Optional;

/** Rejects source code that is not worth starting a sandbox for. */
public final class CodeValidator {

    private CodeValidator() {
    }

    /** @return a finished report when the code is unusable, empty when it may be judged */
    public static Optional<JudgeReport> reject(String code, int totalTests) {
        if (code == null || code.isBlank()) {
            return Optional.of(JudgeReport.compileError(totalTests, FailureMessages.emptySource(), ""));
        }
        if (code.length() > MAX_SOURCE_CHARS) {
            return Optional.of(JudgeReport.compileError(totalTests, FailureMessages.sourceTooLarge(MAX_SOURCE_CHARS), ""));
        }
        return Optional.empty();
    }
}
