import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import toast from "react-hot-toast";
import Button from "@/atoms/Button";
import { formatClock, useAttemptClock } from "@/hooks/useAttemptClock";
import { apiErrorText, testService, type ApiError, type Attempt } from "@/service/testService";

/** The running test: timer, the problems that were drawn, and a way to finish. */
const TestAttempt = () => {
  const { attemptId } = useParams<{ attemptId: string }>();
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setAttempt(await testService.attempt(Number(attemptId)));
    } catch (e) {
      setError((e as ApiError)?.response?.status === 401 ? "Sign in to see this test." : apiErrorText(e, "Test not found."));
    }
  }, [attemptId]);
  useEffect(() => {
    load();
  }, [load]);

  // At zero, ask the server: it is the one that decides whether time is really up.
  const { remaining, elapsed } = useAttemptClock(attempt, load);

  const finish = async () => {
    if (!window.confirm("Finish the test? You will not be able to come back to it.")) return;
    try {
      setAttempt(await testService.finish(Number(attemptId)));
    } catch (e) {
      toast.error(apiErrorText(e, "Could not finish the test"));
    }
  };

  if (error) return <p className="p-12 text-center text-muted-foreground">{error}</p>;
  if (!attempt) return <p className="p-12 text-center text-muted-foreground">Loading…</p>;

  const over = attempt.finished;
  return (
    <main className="mx-auto max-w-3xl px-6 pb-16 pt-12">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-serif text-4xl font-medium tracking-[-0.02em]">{attempt.title}</h1>
          {attempt.description && <p className="mt-1 whitespace-pre-line text-muted-foreground">{attempt.description}</p>}
        </div>
        <div className="shrink-0 text-right">
          {attempt.finished ? (
            <>
              <div className="overline">Time taken</div>
              <div className="font-mono text-3xl">{formatClock(elapsed)}</div>
            </>
          ) : remaining !== null ? (
            <>
              <div className="overline">Time left</div>
              <div className={`font-mono text-3xl ${remaining <= 300 ? "text-danger" : ""}`}>{formatClock(remaining)}</div>
            </>
          ) : (
            <>
              <div className="overline">Elapsed · no time limit</div>
              <div className="font-mono text-3xl">{formatClock(elapsed)}</div>
            </>
          )}
        </div>
      </div>

      <p className="mt-6 text-sm text-muted-foreground">
        {attempt.solved.length} of {attempt.problems.length} solved
      </p>

      {over ? (
        <p className="mt-3 border-l-2 border-border pl-4 text-muted-foreground">
          This test is over. {remaining === 0 ? "Time ran out." : "You finished it."}
        </p>
      ) : (
        <ul className="mt-3 divide-y divide-border border-y border-border">
          {attempt.problems.map((p, i) => (
            <li key={p.id}>
              <Link
                to={`/coding/${p.id}?attempt=${attempt.attemptId}`}
                className="flex items-center justify-between gap-4 py-3 hover:bg-highlight-wash"
              >
                <span>
                  <span className="mr-3 font-mono text-muted-foreground">{i + 1}.</span>
                  {p.title}
                </span>
                <span className="text-[13px] text-muted-foreground">
                  {attempt.solved.includes(p.id) && <span className="text-success">Solved</span>}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {!over && (
        <Button variant="outline" className="mt-6" onClick={finish}>
          Finish test
        </Button>
      )}
    </main>
  );
};

export default TestAttempt;
