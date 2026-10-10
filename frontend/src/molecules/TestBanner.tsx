import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { formatClock, useAttemptClock } from "@/hooks/useAttemptClock";
import { testService, type Attempt } from "@/service/testService";

export const TEST_BANNER_HEIGHT = "2.25rem";

/** Strip above the editor while solving a problem as part of a test: title, countdown, way back. */
const TestBanner = ({ attemptId }: { attemptId: number }) => {
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const load = useCallback(() => {
    testService.attempt(attemptId).then(setAttempt).catch(() => {});
  }, [attemptId]);
  useEffect(load, [load]);
  const left = useAttemptClock(attempt, load);

  if (!attempt) return null;
  return (
    <div
      className="flex items-center justify-between border-b border-border bg-paper-sunken px-4 text-sm"
      style={{ height: TEST_BANNER_HEIGHT }}
    >
      <Link to={`/test/attempt/${attempt.attemptId}`} className="truncate underline">
        {attempt.title}
      </Link>
      {attempt.finished ? (
        <span className="text-danger">Test over</span>
      ) : (
        left !== null && <span className={`font-mono ${left <= 300 ? "text-danger" : ""}`}>{formatClock(left)}</span>
      )}
    </div>
  );
};

export default TestBanner;
