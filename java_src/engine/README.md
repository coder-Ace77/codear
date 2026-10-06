# Engine: how a submission is judged

The engine takes a submission off the queue, runs it in a sandbox, and records a verdict. The code is split so that
each class does one thing, and the parts that decide things never touch Docker.

```
SqsReceiver            takes work off the queue and wires the pieces together (no decisions)
  └─ JudgeService      checks the code, runs the sandbox, asks the resolver for a verdict
       ├─ CodeValidator          rejects empty or oversized source before a sandbox is started
       ├─ JudgeLimitsFactory     time / memory / output limits from the problem and its tests
       ├─ SandboxRunner          (interface) runs code against inputs under limits
       │    └─ DockerSandbox     the Docker implementation
       │         ├─ SandboxWorkspace        scratch folder: source, inputs, runner.sh
       │         ├─ ContainerSpecFactory    environment + Docker restrictions
       │         ├─ ResultArchiveReader     the runner's result files (tar) -> SandboxResult
       │         └─ SandboxJanitor          removes what a crash left behind
       └─ VerdictResolver        SandboxResult + expected outputs -> JudgeReport
            ├─ RunClassifier             which limit did a run hit? (pure table of rules)
            ├─ OutputComparator          does the answer match? (checker package)
            └─ FailureMessages           the sentences a person reads
```

`judge`, `checker` and the record types in `sandbox` are pure: no Docker, no database, no clock. They are covered by
fast unit tests. `DockerJudgeIntegrationTest` runs the whole pipeline against real hostile programs in Docker.

## The sandbox

`src/main/resources/sandbox/runner.sh` runs **inside** the container and does everything that has to happen next to
the untrusted code: compile, then run each test with hard limits. It writes what happened to files in `results/`; the
engine reads those files afterwards. The program's own output never reaches the log stream the engine reads, so it
cannot fake a result. `SandboxProtocol` lists the files, exit codes and environment variables both sides use.

What protects the host, from the outside in:

* Docker: no network, 64 processes, one CPU, memory with no swap, all capabilities dropped except those the runner
  needs, `no-new-privileges`, a small `/tmp`.
* The runner: the program (and the compiler) run as `nobody` with no capabilities; the test inputs and the results
  folder are readable by root only, so a program cannot read other tests' inputs; CPU time, address space, output
  size and wall time are limited per test; the run stops at the first test that crashes or hits a limit.

## Verdicts

`Verdict` says why (`ACCEPTED`, `WRONG_ANSWER`, `COMPILE_ERROR`, `RUNTIME_ERROR`, `TIME_LIMIT_EXCEEDED`,
`MEMORY_LIMIT_EXCEEDED`, `OUTPUT_LIMIT_EXCEEDED`, `SYSTEM_ERROR`). The coarse `RunStatus` (PASSED / FAILED) is kept
for everything that already reads it. Only tests marked as samples ever have their expected output or program errors
shown; everything else is "hidden".

## Comparing answers

`CheckerMode` is stored per problem: `TOKENS` (default: word by word, spacing ignored, case matters),
`TOKENS_IGNORE_CASE`, `EXACT_LINES`, `FLOAT` (numbers within a tolerance).

## Tests

```
./mvnw test                                  # everything; Docker tests skip themselves if Docker or the images are missing
```

To run the Docker tests, build two local images (the names are fixed in the test):

```
docker build -f docker/Dockerfile.python -t codear-python-test docker
# a smaller C++ image than gcc:latest is enough:
printf 'FROM debian:bookworm-slim\nRUN apt-get update && apt-get install -y --no-install-recommends g++ libc6-dev time && rm -rf /var/lib/apt/lists/*\nWORKDIR /app\n' | docker build -t codear-cpp-test -
```

Against a deployed system, `python3 -m scripts.judge_selftest` (repo root) submits about 30 hostile programs through
the API and checks each verdict.
