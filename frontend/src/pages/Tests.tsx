import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import Button from "@/atoms/Button";
import type { TestSummary } from "@/service/adminService";
import { apiErrorText, testService, type ApiError } from "@/service/testService";

/** Public tests anyone signed in can start. */
const Tests = () => {
  const navigate = useNavigate();
  const [tests, setTests] = useState<TestSummary[] | null>(null);
  const [needsLogin, setNeedsLogin] = useState(false);
  const [starting, setStarting] = useState<number | null>(null);

  useEffect(() => {
    testService
      .publicTests()
      .then(setTests)
      .catch((e) => {
        if ((e as ApiError)?.response?.status === 401) setNeedsLogin(true);
        else toast.error(apiErrorText(e, "Failed to load tests"));
        setTests([]);
      });
  }, []);

  const start = async (test: TestSummary) => {
    const warning = test.durationMinutes
      ? `Start "${test.title}"? The ${test.durationMinutes} minute timer begins now and you only get one attempt.`
      : `Start "${test.title}"? You only get one attempt.`;
    if (!window.confirm(warning)) return;
    setStarting(test.id);
    try {
      const attempt = await testService.startPublic(test.id);
      navigate(`/test/attempt/${attempt.attemptId}`);
    } catch (e) {
      toast.error(apiErrorText(e, "Could not start the test"));
    } finally {
      setStarting(null);
    }
  };

  return (
    <main className="mx-auto max-w-3xl px-6 pb-16 pt-12">
      <h1 className="font-serif text-5xl font-medium leading-[1.05] tracking-[-0.02em]">Tests</h1>
      <p className="mt-1 text-muted-foreground">Timed sets of problems, drawn at random when you start.</p>

      <div className="mt-8">
        {tests === null ? (
          <p className="text-muted-foreground">Loading…</p>
        ) : needsLogin ? (
          <p>
            <Link to="/login?next=/tests" className="underline">Sign in</Link> to see the available tests.
          </p>
        ) : tests.length === 0 ? (
          <p className="text-muted-foreground">No public tests right now.</p>
        ) : (
          <ul className="divide-y divide-border border-y border-border">
            {tests.map((t) => (
              <li key={t.id} className="flex items-center justify-between gap-4 py-4">
                <div className="min-w-0">
                  <h2 className="font-serif text-xl">{t.title}</h2>
                  {t.description && <p className="mt-0.5 text-sm text-muted-foreground">{t.description}</p>}
                  <p className="mt-1 text-[13px] text-muted-foreground">
                    {t.problemCount} problem{t.problemCount === 1 ? "" : "s"} ·{" "}
                    {t.durationMinutes ? `${t.durationMinutes} minutes` : "no time limit"}
                  </p>
                </div>
                <Button onClick={() => start(t)} disabled={starting === t.id}>
                  {starting === t.id ? "Starting…" : "Start"}
                </Button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </main>
  );
};

export default Tests;
