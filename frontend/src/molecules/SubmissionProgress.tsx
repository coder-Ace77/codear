import { useMemo } from "react";
import Verdict from "@/atoms/Verdict";
import { showsTestCount, styleFor, verdictKey } from "@/lib/verdict";
import type { SubmissionProgress as Progress, ProgressStage } from "@/types/submissionProgress";
import type { WatchPhase } from "@/hooks/useSubmissionProgress";

const STEPS: { stage: ProgressStage; label: string }[] = [
  { stage: "QUEUED", label: "Queued" },
  { stage: "PREPARING", label: "Preparing" },
  { stage: "RUNNING", label: "Running" },
  { stage: "JUDGING", label: "Checking" },
  { stage: "DONE", label: "Done" },
];

/** Beyond this many tests, per-test cells would just be noise: the bar alone carries it. */
const MAX_CELLS = 60;

interface Props {
  progress: Progress;
  phase: WatchPhase;
  connectionLost: boolean;
  slow: boolean;
  error: string | null;
  onDismiss: () => void;
  onViewSubmissions: () => void;
}

const SubmissionProgress = ({ progress, phase, connectionLost, slow, error, onDismiss, onViewSubmissions }: Props) => {
  const finished = progress.terminal;
  const failedToJudge = progress.stage === "ERROR";
  const result = progress.result;
  const percent = finished ? 100 : progress.percent;
  const stepIndex = Math.max(0, STEPS.findIndex((s) => s.stage === (failedToJudge ? "DONE" : progress.stage)));

  // Screen readers hear stage changes and the verdict, not every test number.
  const announcement = useMemo(() => {
    if (finished && failedToJudge) return "Judging failed";
    if (finished) {
      return result?.status === "PASSED"
        ? `Accepted. ${result.passedTests} of ${result.totalTests} tests passed`
        : `${styleFor(result ? verdictKey(result) : "FAILED").label}.`;
    }
    return STEPS.find((s) => s.stage === progress.stage)?.label ?? "Judging";
  }, [finished, failedToJudge, result, progress.stage]);

  const cells = progress.total && progress.total <= MAX_CELLS ? progress.total : 0;

  return (
    <section aria-label="Submission progress" className="rounded-md border border-border bg-card p-4">
      <span className="sr-only" aria-live="polite">
        {announcement}
      </span>

      <div className="flex items-baseline justify-between gap-3">
        <p className="text-sm font-medium">{finished ? (failedToJudge ? "Judging failed" : "Finished") : progress.message}</p>
        <span className="font-mono text-[13px] text-muted-foreground">{percent}%</span>
      </div>

      <div
        role="progressbar"
        aria-label="Judging progress"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={percent}
        className="mt-2 h-1.5 w-full bg-secondary"
      >
        <div className="h-full bg-foreground transition-[width] duration-300 ease-out" style={{ width: `${percent}%` }} />
      </div>

      {cells > 0 && (
        <div className="mt-3 flex flex-wrap gap-1" aria-hidden="true">
          {Array.from({ length: cells }, (_, i) => {
            const number = i + 1;
            const done = finished || number <= progress.completed;
            const running = !finished && !done && number === progress.started;
            return (
              <span
                key={number}
                title={`Test ${number}`}
                className={`block h-3 w-3 border ${
                  done ? "border-foreground bg-foreground" : running ? "border-highlight bg-highlight" : "border-input"
                }`}
              />
            );
          })}
        </div>
      )}

      <ol className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-[12px]" aria-hidden="true">
        {STEPS.map((step, i) => (
          <li
            key={step.stage}
            className={i < stepIndex ? "text-muted-foreground" : i === stepIndex ? "font-semibold text-foreground" : "text-muted-foreground opacity-60"}
          >
            {i < stepIndex ? "✓ " : ""}
            {step.label}
          </li>
        ))}
      </ol>

      {connectionLost && !finished && (
        <p className="mt-3 text-[13px] text-warning" role="status">
          Connection lost. Trying again…
        </p>
      )}
      {slow && !connectionLost && !finished && (
        <p className="mt-3 text-[13px] text-warning" role="status">
          This is taking longer than usual. The judge may be busy, and we will keep checking.
        </p>
      )}
      {phase === "failed" && error && (
        <p className="mt-3 text-[13px] text-danger" role="alert">
          {error}
        </p>
      )}

      {finished && (
        <div className="mt-4 border-t border-border pt-3">
          {failedToJudge ? (
            <p className="text-[13px] text-danger">
              The judge hit an internal error while checking this submission. Submit again in a moment.
            </p>
          ) : result ? (
            <>
              <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
                <Verdict status={verdictKey(result)} />
                {showsTestCount(verdictKey(result)) && (
                  <span className="font-mono text-[13px]">
                    <strong className="font-medium">
                      {result.passedTests}/{result.totalTests}
                    </strong>{" "}
                    tests
                  </span>
                )}
                {result.timeTakenMs !== null && result.timeTakenMs !== undefined && showsTestCount(verdictKey(result)) && (
                  <span className="font-mono text-[13px]">{result.timeTakenMs} ms</span>
                )}
                {result.memoryUsed && showsTestCount(verdictKey(result)) && (
                  <span className="font-mono text-[13px]">{result.memoryUsed}</span>
                )}
              </div>
              {verdictKey(result) !== "ACCEPTED" && result.status !== "PASSED" && result.result && (
                <pre className="mt-2 max-h-40 overflow-auto whitespace-pre-wrap rounded-md border border-border bg-secondary p-3 font-mono text-[12px] leading-5">
                  {result.result}
                </pre>
              )}
            </>
          ) : null}
        </div>
      )}

      {(finished || phase === "failed") && (
        <div className="mt-3 flex gap-3">
          <button
            onClick={onViewSubmissions}
            className="inline-flex h-7 items-center rounded-sm border border-input px-3 text-[13px] font-semibold transition-colors hover:border-foreground hover:bg-highlight-wash"
          >
            View in submissions
          </button>
          <button
            onClick={onDismiss}
            className="inline-flex h-7 items-center px-1 text-[13px] font-medium text-muted-foreground underline underline-offset-[3px] hover:text-foreground"
          >
            Dismiss
          </button>
        </div>
      )}
    </section>
  );
};

export default SubmissionProgress;
