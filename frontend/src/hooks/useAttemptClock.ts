import { useEffect, useRef, useState } from "react";
import type { Attempt } from "@/service/testService";

/**
 * Seconds left in an attempt, counted down locally from what the server said (so a wrong clock on the
 * user's machine cannot change it). null when untimed. Calls onExpire once when it reaches zero.
 */
export const useAttemptClock = (attempt: Attempt | null, onExpire?: () => void) => {
  const [left, setLeft] = useState<number | null>(null);
  const expire = useRef(onExpire);
  expire.current = onExpire;

  useEffect(() => {
    if (!attempt || attempt.secondsRemaining === null || attempt.finished) {
      setLeft(attempt?.finished && attempt.secondsRemaining !== null ? 0 : null);
      return;
    }
    const deadline = Date.now() + attempt.secondsRemaining * 1000;
    let fired = false;
    const tick = () => {
      const s = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
      setLeft(s);
      if (s === 0 && !fired) {
        fired = true;
        expire.current?.();
      }
    };
    tick();
    const timer = setInterval(tick, 1000);
    return () => clearInterval(timer);
  }, [attempt]);

  return left;
};

export const formatClock = (seconds: number) => {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  const mm = String(m).padStart(2, "0");
  const ss = String(s).padStart(2, "0");
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
};
