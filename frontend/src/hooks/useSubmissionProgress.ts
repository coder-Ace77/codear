import { useCallback, useEffect, useRef, useState } from "react";
import axios from "axios";
import { codingService } from "@/service/codingService";
import type { SubmissionProgress } from "@/types/submissionProgress";

export type WatchPhase = "idle" | "watching" | "done" | "failed";

/** After this long without a verdict, tell the person it is slower than usual. */
const SLOW_AFTER_MS = 60_000;
/** Stop trying after this long: the judge has lost the submission or the network is gone. */
const GIVE_UP_AFTER_MS = 10 * 60_000;

const sleep = (ms: number, signal: AbortSignal) =>
  new Promise<void>((resolve) => {
    const id = window.setTimeout(resolve, ms);
    signal.addEventListener("abort", () => {
      window.clearTimeout(id);
      resolve();
    });
  });

/**
 * Follows one submission to its verdict with the server's long-poll. Each request returns the moment the
 * engine reports something new, so progress arrives as it happens with no fixed timer, and a request that
 * times out with nothing new is simply asked again with the same version.
 */
export const useSubmissionProgress = () => {
  const [progress, setProgress] = useState<SubmissionProgress | null>(null);
  const [phase, setPhase] = useState<WatchPhase>("idle");
  const [connectionLost, setConnectionLost] = useState(false);
  const [slow, setSlow] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const controller = useRef<AbortController | null>(null);

  const stop = useCallback(() => {
    controller.current?.abort();
    controller.current = null;
  }, []);

  const reset = useCallback(() => {
    stop();
    setProgress(null);
    setPhase("idle");
    setConnectionLost(false);
    setSlow(false);
    setError(null);
  }, [stop]);

  const start = useCallback(
    (submissionId: string, onFinished?: (final: SubmissionProgress) => void) => {
      stop();
      const ctrl = new AbortController();
      controller.current = ctrl;

      setPhase("watching");
      setConnectionLost(false);
      setSlow(false);
      setError(null);
      // show the bar immediately instead of waiting for the first answer
      setProgress({
        submissionId, version: 0, stage: "QUEUED", message: "Queued", percent: 5,
        total: null, started: 0, completed: 0, terminal: false, changed: true, status: "IN_PROGRESS",
      });

      (async () => {
        const startedAt = Date.now();
        let since = 0;
        let failures = 0;

        while (!ctrl.signal.aborted) {
          try {
            const answer = await codingService.getSubmissionProgress(submissionId, since, ctrl.signal);
            failures = 0;
            setConnectionLost(false);
            setSlow(Date.now() - startedAt > SLOW_AFTER_MS && !answer.terminal);

            if (answer.changed) {
              since = Math.max(since, answer.version);
              setProgress(answer);
            }
            if (answer.terminal) {
              setPhase("done");
              onFinished?.(answer);
              return;
            }
            // a server that answered at once with nothing new would otherwise turn this into a busy loop
            if (!answer.changed) await sleep(300, ctrl.signal);
          } catch (e) {
            if (ctrl.signal.aborted || axios.isCancel(e)) return;

            const status = axios.isAxiosError(e) ? e.response?.status : undefined;
            if (status === 404) {
              setError("This submission could not be found.");
              setPhase("failed");
              return;
            }
            if (status === 401) {
              setError("You were signed out. Sign in again to see the result.");
              setPhase("failed");
              return;
            }
            if (Date.now() - startedAt > GIVE_UP_AFTER_MS) {
              setError("We lost track of this submission. Check the Submissions tab in a moment.");
              setPhase("failed");
              return;
            }
            failures += 1;
            setConnectionLost(true);
            await sleep(Math.min(1000 * 2 ** Math.min(failures, 3), 8000), ctrl.signal);
          }
        }
      })();
    },
    [stop]
  );

  // leaving the page cancels the request in flight
  useEffect(() => stop, [stop]);

  return { progress, phase, connectionLost, slow, error, start, reset };
};
