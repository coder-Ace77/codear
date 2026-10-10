import { useState } from "react";
import { formatClock } from "@/hooks/useAttemptClock";
import { formatDuration } from "@/lib/time";
import type { AttemptResult, TestHistory } from "@/service/adminService";

const OUTCOME = {
  FINISHED: "Finished",
  TIME_UP: "Time ran out",
  IN_PROGRESS: "In progress",
} as const;

const Tile = ({ label, value }: { label: string; value: React.ReactNode }) => (
  <div className="border-t border-border pt-2">
    <dt className="overline">{label}</dt>
    <dd className="font-mono text-2xl font-medium">{value}</dd>
  </div>
);

const AttemptRow = ({ a }: { a: AttemptResult }) => {
  const [open, setOpen] = useState(false);
  const started = new Date(a.startedAt);
  return (
    <li>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        className="grid w-full grid-cols-[minmax(0,1fr)_auto] items-center gap-x-4 gap-y-0.5 py-3 text-left hover:bg-highlight-wash sm:grid-cols-[minmax(0,1fr)_6rem_7rem_8rem]"
      >
        <span className="min-w-0">
          <span className="block truncate font-medium">{a.testTitle}</span>
          <span className="text-[13px] text-muted-foreground">
            {started.toLocaleDateString()} {started.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
          </span>
        </span>
        <span className="font-mono">
          {a.solvedCount}/{a.total} <span className="text-[13px] text-muted-foreground">solved</span>
        </span>
        <span className="font-mono text-sm" title="Time taken">
          {formatClock(a.timeTakenSeconds)}
          {a.durationMinutes ? <span className="text-muted-foreground"> / {a.durationMinutes}m</span> : null}
        </span>
        <span className={`text-[13px] ${a.outcome === "TIME_UP" ? "text-danger" : "text-muted-foreground"}`}>
          {OUTCOME[a.outcome]}
        </span>
      </button>
      {open && (
        <ul className="mb-3 space-y-1 border-l-2 border-border pl-4 text-sm">
          {a.problems.map((p, i) => (
            <li key={p.id} className="flex flex-wrap gap-x-3">
              <span className="font-mono text-muted-foreground">{i + 1}.</span>
              <span className={p.solved ? "text-success" : "text-muted-foreground"}>{p.solved ? "✓" : "✗"}</span>
              <span>{p.title}</span>
              <span className="text-[13px] text-muted-foreground">
                {p.submissions} submission{p.submissions === 1 ? "" : "s"}
                {p.solvedAt &&
                  ` · solved after ${formatDuration(
                    Math.max(0, Math.round((new Date(p.solvedAt).getTime() - started.getTime()) / 1000)),
                  )}`}
              </span>
            </li>
          ))}
        </ul>
      )}
    </li>
  );
};

/** Totals and the list of past attempts; click an attempt for its problems. */
const TestHistoryView = ({ history, emptyText }: { history: TestHistory; emptyText: string }) => {
  if (history.attempts.length === 0) return <p className="text-sm text-muted-foreground">{emptyText}</p>;
  const rate = history.problemsGiven ? Math.round((100 * history.problemsSolved) / history.problemsGiven) : 0;
  return (
    <div className="space-y-6">
      <dl className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Tile label="Tests taken" value={history.attemptsTaken} />
        <Tile label="Problems solved" value={`${history.problemsSolved}/${history.problemsGiven}`} />
        <Tile label="Solve rate" value={`${rate}%`} />
        <Tile
          label="Average time"
          value={history.averageTimeSeconds === null ? "–" : formatDuration(history.averageTimeSeconds)}
        />
      </dl>
      <ul className="divide-y divide-border border-y border-border">
        {history.attempts.map((a) => (
          <AttemptRow key={a.attemptId} a={a} />
        ))}
      </ul>
    </div>
  );
};

export default TestHistoryView;
