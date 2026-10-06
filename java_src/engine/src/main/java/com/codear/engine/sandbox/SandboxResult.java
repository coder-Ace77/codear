package com.codear.engine.sandbox;

import java.util.List;
import java.util.Optional;

/**
 * Everything the sandbox recorded for one submission.
 *
 * @param finished  the runner reached its end (false when the container was cut off)
 * @param oomKilled the container was killed for using too much memory
 */
public record SandboxResult(CompileResult compile, List<TestRun> runs, boolean finished, boolean oomKilled) {

    public Optional<TestRun> runFor(int index) {
        return runs.stream().filter(run -> run.index() == index).findFirst();
    }
}
