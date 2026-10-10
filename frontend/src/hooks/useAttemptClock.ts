import { useEffect, useRef, useState } from "react";
import type { Attempt } from "@/service/testService";

export interface AttemptClock {
  /** Seconds left; null when the test is untimed. */
  remaining: number | null;
  /** Seconds since the attempt started, frozen once it is over. */
  elapsed: number;
}

/**
 * Counts locally from the numbers the server sent (so a wrong clock on the user's machine changes nothing):
 * down for timed tests, up for every test. Calls onExpire once when a timed test reaches zero.
 */
export const useAttemptClock = (attempt: Attempt | null, onExpire?: () => void): AttemptClock => {
  const [clock, setClock] = useState<AttemptClock>({ remaining: null, elapsed: 0 });
  const expire = useRef(onExpire);
  expire.current = onExpire;

  useEffect(() => {
    if (!attempt) return;
    const elapsedAtLoad = attempt.secondsElapsed ?? 0;
    if (attempt.finished) {
      // as the server left it: 0 means time ran out, more means it was finished early
      setClock({ remaining: attempt.secondsRemaining, elapsed: elapsedAtLoad });
      return;
    }
    const loadedAt = Date.now();
    let fired = false;
    const tick = () => {
      const passed = Math.floor((Date.now() - loadedAt) / 1000);
      const remaining = attempt.secondsRemaining === null ? null : Math.max(0, attempt.secondsRemaining - passed);
      setClock({ remaining, elapsed: elapsedAtLoad + passed });
      if (remaining === 0 && !fired) {
        fired = true;
        expire.current?.();
      }
    };
    tick();
    const timer = setInterval(tick, 1000);
    return () => clearInterval(timer);
  }, [attempt]);

  return clock;
};

export const formatClock = (seconds: number) => {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  const mm = String(m).padStart(2, "0");
  const ss = String(s).padStart(2, "0");
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
};
