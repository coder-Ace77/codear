import type { SubmissionStatus } from "@/types/submission";
import { cn } from "@/lib/utils";

const states: Record<string, { label: string; tone: string; glyph: "check" | "cross" | "clock" | "ring" }> = {
  PASSED: { label: "Accepted", tone: "bg-success-wash text-success", glyph: "check" },
  COMPLETED: { label: "Completed", tone: "bg-success-wash text-success", glyph: "check" },
  FAILED: { label: "Failed", tone: "bg-danger-wash text-danger", glyph: "cross" },
  ERROR: { label: "Runtime error", tone: "bg-danger-wash text-danger", glyph: "cross" },
  IN_PROGRESS: { label: "Running", tone: "bg-secondary text-info", glyph: "ring" },
  RUNNING: { label: "Running", tone: "bg-secondary text-info", glyph: "ring" },
  PENDING: { label: "Queued", tone: "bg-secondary text-info", glyph: "clock" },
};

const Glyph = ({ kind }: { kind: "check" | "cross" | "clock" | "ring" }) => (
  <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="square" aria-hidden="true" className="shrink-0">
    {kind === "check" && <path d="M2 6.4l2.6 2.6L10 3.4" />}
    {kind === "cross" && <path d="M2.5 2.5l7 7M9.5 2.5l-7 7" />}
    {kind === "clock" && (
      <>
        <circle cx="6" cy="6" r="4.6" />
        <path d="M6 3.4V6l1.8 1.1" />
      </>
    )}
    {kind === "ring" && <circle cx="6" cy="6" r="4.6" strokeDasharray="3 2" />}
  </svg>
);

/** A glyph, a word and a wash: never colour alone. */
const Verdict = ({ status, className }: { status: SubmissionStatus; className?: string }) => {
  const s = states[status] ?? states.PENDING;
  return (
    <span className={cn("inline-flex items-center gap-1.5 whitespace-nowrap rounded-sm px-2 py-0.5 font-mono text-xs font-medium", s.tone, className)}>
      <Glyph kind={s.glyph} />
      {s.label}
    </span>
  );
};

export default Verdict;
