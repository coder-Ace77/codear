import Verdict from "@/atoms/Verdict";
import { showsTestCount, styleFor, verdictKey } from "@/lib/verdict";
import type { Submission } from "@/types/submission";
import type { WatchPhase } from "@/hooks/useSubmissionResult";

interface Props {
  result: Submission | null;
  phase: WatchPhase;
  connectionLost: boolean;
  slow: boolean;
  error: string | null;
  onDismiss: () => void;
  /** Left out during a test, which has no submissions page. */
  onViewSubmissions?: () => void;
}

const SubmissionResultPanel = ({ result, phase, connectionLost, slow, error, onDismiss, onViewSubmissions }: Props) => {
  const finished = result !== null;

  // Screen readers hear when judging starts and the verdict.
  const announcement = !result
    ? phase === "failed" ? "No result" : "Judging"
    : result.status === "PASSED"
      ? `Accepted. ${result.passedTests} of ${result.totalTests} tests passed`
      : `${styleFor(verdictKey(result)).label}.`;

  return (
    <section aria-label="Submission result" className="rounded-md border border-border bg-card p-4">
      <span className="sr-only" aria-live="polite">
        {announcement}
      </span>

      <p className="text-sm font-medium">{finished ? "Finished" : phase === "failed" ? "No result" : "Judging…"}</p>

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

      {result && (
        <div className="mt-4 border-t border-border pt-3">
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
        </div>
      )}

      {(finished || phase === "failed") && (
        <div className="mt-3 flex gap-3">
          {onViewSubmissions && <button
            onClick={onViewSubmissions}
            className="inline-flex h-7 items-center rounded-sm border border-input px-3 text-[13px] font-semibold transition-colors hover:border-foreground hover:bg-highlight-wash"
          >
            View in submissions
          </button>}
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

export default SubmissionResultPanel;
