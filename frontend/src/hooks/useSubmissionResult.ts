import { useCallback, useEffect, useRef, useState } from "react";
import axios from "axios";
import { codingService } from "@/service/codingService";
import type { Submission } from "@/types/submission";

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
 * Follows one submission to its verdict. The server holds each request for up to 10 seconds while the judge
 * works, so the hook simply asks again whenever an answer comes back still IN_PROGRESS.
 */
export const useSubmissionResult = () => {
  const [result, setResult] = useState<Submission | null>(null);
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
    setResult(null);
    setPhase("idle");
    setConnectionLost(false);
    setSlow(false);
    setError(null);
  }, [stop]);

  const start = useCallback(
    (submissionId: string, onFinished?: (final: Submission) => void) => {
      stop();
      const ctrl = new AbortController();
      controller.current = ctrl;

      setResult(null);
      setPhase("watching");
      setConnectionLost(false);
      setSlow(false);
      setError(null);

      (async () => {
        const startedAt = Date.now();
        let failures = 0;

        while (!ctrl.signal.aborted) {
          try {
            const answer = await codingService.getSubmissionStatus(submissionId, ctrl.signal);
            failures = 0;
            setConnectionLost(false);

            if (answer.status !== "IN_PROGRESS") {
              setResult(answer);
              setSlow(false);
              setPhase("done");
              onFinished?.(answer);
              return;
            }
            setSlow(Date.now() - startedAt > SLOW_AFTER_MS);
            // a server that answered at once without waiting would otherwise turn this into a busy loop
            await sleep(500, ctrl.signal);
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

  return { result, phase, connectionLost, slow, error, start, reset };
};
