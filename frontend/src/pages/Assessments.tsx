import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import Button from "@/atoms/Button";
import { formatClock } from "@/hooks/useAttemptClock";
import AssessmentHistoryView from "@/molecules/AssessmentHistoryView";
import type { AssessmentHistory } from "@/service/adminService";
import { apiErrorText, assessmentService, type ActiveAttempt, type ApiError, type PublicAssessment } from "@/service/assessmentService";

/** Public assessments anyone signed in can start. */
const Assessments = () => {
  const navigate = useNavigate();
  const [assessments, setAssessments] = useState<PublicAssessment[] | null>(null);
  const [needsLogin, setNeedsLogin] = useState(false);
  const [starting, setStarting] = useState<number | null>(null);
  const [active, setActive] = useState<ActiveAttempt[]>([]);
  const [, setTick] = useState(0);
  const [history, setHistory] = useState<AssessmentHistory | null>(null);
  const loadHistory = () => assessmentService.history().then(setHistory).catch(() => {});
  useEffect(() => {
    loadHistory();
  }, []);

  const loadActive = () => assessmentService.activeAttempts().then(setActive).catch(() => {});
  useEffect(() => {
    loadActive();
    const timer = setInterval(() => setTick((t) => t + 1), 1000); // redraw the countdowns
    return () => clearInterval(timer);
  }, []);

  const finish = async (a: ActiveAttempt) => {
    if (!window.confirm(`Finish "${a.title}"? You will not be able to come back to it.`)) return;
    try {
      await assessmentService.finish(a.attemptId);
      loadActive();
      loadAssessments(); // its Start button may now be Done
      loadHistory();
    } catch (e) {
      toast.error(apiErrorText(e, "Could not finish the assessment"));
    }
  };

  const loadAssessments = () =>
    assessmentService
      .publicAssessments()
      .then(setAssessments)
      .catch((e) => {
        if ((e as ApiError)?.response?.status === 401) setNeedsLogin(true);
        else toast.error(apiErrorText(e, "Failed to load assessments"));
        setAssessments([]);
      });
  useEffect(() => {
    loadAssessments();
  }, []);

  const start = async (assessment: PublicAssessment) => {
    const attempts = assessment.multipleAttempts ? "You can take it again later." : "You only get one attempt.";
    const warning = assessment.durationMinutes
      ? `Start "${assessment.title}"? The ${assessment.durationMinutes} minute timer begins now. ${attempts}`
      : `Start "${assessment.title}"? ${attempts}`;
    if (!window.confirm(warning)) return;
    setStarting(assessment.id);
    try {
      const attempt = await assessmentService.startPublic(assessment.id);
      navigate(`/assessment/attempt/${attempt.attemptId}`);
    } catch (e) {
      toast.error(apiErrorText(e, "Could not start the assessment"));
    } finally {
      setStarting(null);
    }
  };

  return (
    <main className="mx-auto max-w-3xl px-6 pb-16 pt-12">
      <h1 className="font-serif text-5xl font-medium leading-[1.05] tracking-[-0.02em]">Assessments</h1>
      <p className="mt-1 text-muted-foreground">Timed sets of problems, drawn at random when you start.</p>

      {active.length > 0 && (
        <section className="mt-8">
          <h2 className="overline mb-2">In progress</h2>
          <ul className="divide-y divide-border border-y border-border">
            {active.map((a) => {
              const left =
                a.expiresAt === null ? null : Math.max(0, Math.round((new Date(a.expiresAt).getTime() - Date.now()) / 1000));
              return (
                <li key={a.attemptId} className="flex items-center justify-between gap-4 py-3">
                  <div className="min-w-0">
                    <span className="block truncate font-medium">{a.title}</span>
                    <span className="text-[13px] text-muted-foreground">
                      {a.solved} of {a.total} solved{left !== null ? ` · ${formatClock(left)} left` : " · no time limit"}
                    </span>
                  </div>
                  <div className="flex gap-2">
                    <Link to={`/assessment/attempt/${a.attemptId}`}>
                      <Button size="sm">Resume</Button>
                    </Link>
                    <Button size="sm" variant="outline" onClick={() => finish(a)}>
                      Finish
                    </Button>
                  </div>
                </li>
              );
            })}
          </ul>
        </section>
      )}

      <div className="mt-8">
        {assessments === null ? (
          <p className="text-muted-foreground">Loading…</p>
        ) : needsLogin ? (
          <p>
            <Link to="/login?next=/assessments" className="underline">Sign in</Link> to see the available assessments.
          </p>
        ) : assessments.length === 0 ? (
          <p className="text-muted-foreground">No public assessments right now.</p>
        ) : (
          <ul className="divide-y divide-border border-y border-border">
            {assessments.map((t) => (
              <li key={t.id} className="flex items-center justify-between gap-4 py-4">
                <div className="min-w-0">
                  <h2 className="font-serif text-xl">{t.title}</h2>
                  {t.description && <p className="mt-0.5 text-sm text-muted-foreground">{t.description}</p>}
                  <p className="mt-1 text-[13px] text-muted-foreground">
                    {t.problemCount} problem{t.problemCount === 1 ? "" : "s"} ·{" "}
                    {t.durationMinutes ? `${t.durationMinutes} minutes` : "no time limit"} ·{" "}
                    {t.multipleAttempts ? "multiple attempts" : "one attempt"}
                  </p>
                </div>
                {t.myStatus === "DONE" ? (
                  <Button disabled title="You have already taken this assessment">
                    Done
                  </Button>
                ) : t.myStatus === "IN_PROGRESS" && t.myAttemptId ? (
                  <Link to={`/assessment/attempt/${t.myAttemptId}`}>
                    <Button>Resume</Button>
                  </Link>
                ) : (
                  <Button onClick={() => start(t)} disabled={starting === t.id}>
                    {starting === t.id ? "Starting…" : t.myStatus === "RETAKE" ? "Take again" : "Start"}
                  </Button>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      {history && (
        <section className="mt-12">
          <h2 className="mb-4 font-serif text-2xl">Your history</h2>
          <AssessmentHistoryView history={history} emptyText="Assessments you finish will show up here." />
        </section>
      )}
    </main>
  );
};

export default Assessments;
