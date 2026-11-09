import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { formatClock, useAttemptClock } from "@/hooks/useAttemptClock";
import { apiErrorText, assessmentService, type Attempt } from "@/service/assessmentService";

export const ASSESSMENT_BANNER_HEIGHT = "2.5rem";

/** Strip above the editor while solving a problem as part of an assessment: title, countdown and Finish. */
const AssessmentBanner = ({ attemptId }: { attemptId: number }) => {
  const navigate = useNavigate();
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const load = useCallback(() => {
    assessmentService.attempt(attemptId).then(setAttempt).catch(() => {});
  }, [attemptId]);
  useEffect(load, [load]);
  const { remaining, elapsed } = useAttemptClock(attempt, load);

  const finish = async () => {
    if (!window.confirm("Finish the assessment? You will not be able to come back to it.")) return;
    try {
      await assessmentService.finish(attemptId);
      navigate(`/assessment/attempt/${attemptId}`);
    } catch (e) {
      toast.error(apiErrorText(e, "Could not finish the assessment"));
    }
  };

  if (!attempt) return null;
  return (
    <div
      className="flex items-center justify-between gap-4 border-b border-border bg-paper-sunken px-4 text-sm"
      style={{ height: ASSESSMENT_BANNER_HEIGHT }}
    >
      <Link to={`/assessment/attempt/${attempt.attemptId}`} className="truncate underline">
        {attempt.title}
      </Link>
      <div className="flex items-center gap-4">
        {attempt.finished ? (
          <span className="text-danger">Assessment over</span>
        ) : (
          <>
            {remaining !== null ? (
              <span className={`font-mono ${remaining <= 300 ? "text-danger" : ""}`} title="Time left">
                {formatClock(remaining)} left
              </span>
            ) : (
              <span className="font-mono text-muted-foreground" title="No time limit">
                {formatClock(elapsed)} elapsed
              </span>
            )}
            <button
              onClick={finish}
              className="inline-flex h-7 items-center rounded-sm border border-input px-3 text-[13px] font-semibold transition-colors hover:border-foreground hover:bg-highlight-wash"
            >
              Finish assessment
            </button>
          </>
        )}
      </div>
    </div>
  );
};

export default AssessmentBanner;
