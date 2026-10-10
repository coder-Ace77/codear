import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { formatClock, useAttemptClock } from "@/hooks/useAttemptClock";
import { apiErrorText, testService, type Attempt } from "@/service/testService";

export const TEST_BANNER_HEIGHT = "2.5rem";

/** Strip above the editor while solving a problem as part of a test: title, countdown and Finish. */
const TestBanner = ({ attemptId }: { attemptId: number }) => {
  const navigate = useNavigate();
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const load = useCallback(() => {
    testService.attempt(attemptId).then(setAttempt).catch(() => {});
  }, [attemptId]);
  useEffect(load, [load]);
  const left = useAttemptClock(attempt, load);

  const finish = async () => {
    if (!window.confirm("Finish the test? You will not be able to come back to it.")) return;
    try {
      await testService.finish(attemptId);
      navigate(`/test/attempt/${attemptId}`);
    } catch (e) {
      toast.error(apiErrorText(e, "Could not finish the test"));
    }
  };

  if (!attempt) return null;
  return (
    <div
      className="flex items-center justify-between gap-4 border-b border-border bg-paper-sunken px-4 text-sm"
      style={{ height: TEST_BANNER_HEIGHT }}
    >
      <Link to={`/test/attempt/${attempt.attemptId}`} className="truncate underline">
        {attempt.title}
      </Link>
      <div className="flex items-center gap-4">
        {attempt.finished ? (
          <span className="text-danger">Test over</span>
        ) : (
          <>
            {left !== null && <span className={`font-mono ${left <= 300 ? "text-danger" : ""}`}>{formatClock(left)}</span>}
            <button
              onClick={finish}
              className="inline-flex h-7 items-center rounded-sm border border-input px-3 text-[13px] font-semibold transition-colors hover:border-foreground hover:bg-highlight-wash"
            >
              Finish test
            </button>
          </>
        )}
      </div>
    </div>
  );
};

export default TestBanner;
