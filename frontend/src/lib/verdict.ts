/** What the judge can say about a submission, and how each answer looks. One table, so every screen agrees. */

export type Glyph = "check" | "cross" | "clock" | "ring";

export interface VerdictStyle {
  label: string;
  tone: string;
  glyph: Glyph;
}

const GOOD = "bg-success-wash text-success";
const BAD = "bg-danger-wash text-danger";
const LIMIT = "bg-highlight-wash text-warning";
const NEUTRAL = "bg-secondary text-muted-foreground";
const WORKING = "bg-secondary text-info";

export const VERDICT_STYLES: Record<string, VerdictStyle> = {
  // what the judge decided
  ACCEPTED: { label: "Accepted", tone: GOOD, glyph: "check" },
  WRONG_ANSWER: { label: "Wrong answer", tone: BAD, glyph: "cross" },
  COMPILE_ERROR: { label: "Compile error", tone: BAD, glyph: "cross" },
  RUNTIME_ERROR: { label: "Runtime error", tone: BAD, glyph: "cross" },
  TIME_LIMIT_EXCEEDED: { label: "Time limit", tone: LIMIT, glyph: "clock" },
  MEMORY_LIMIT_EXCEEDED: { label: "Memory limit", tone: LIMIT, glyph: "clock" },
  OUTPUT_LIMIT_EXCEEDED: { label: "Output limit", tone: LIMIT, glyph: "clock" },
  SYSTEM_ERROR: { label: "System error", tone: NEUTRAL, glyph: "cross" },

  // the coarse status, for submissions judged before verdicts existed
  PASSED: { label: "Accepted", tone: GOOD, glyph: "check" },
  COMPLETED: { label: "Completed", tone: GOOD, glyph: "check" },
  FAILED: { label: "Failed", tone: BAD, glyph: "cross" },
  ERROR: { label: "Runtime error", tone: BAD, glyph: "cross" },

  // still being worked on, or never finished
  IN_PROGRESS: { label: "Running", tone: WORKING, glyph: "ring" },
  RUNNING: { label: "Running", tone: WORKING, glyph: "ring" },
  PENDING: { label: "Queued", tone: WORKING, glyph: "clock" },
  STALLED: { label: "No result", tone: LIMIT, glyph: "clock" },
};

const UNKNOWN: VerdictStyle = { label: "Unknown", tone: NEUTRAL, glyph: "clock" };

export const styleFor = (key: string | null | undefined): VerdictStyle => VERDICT_STYLES[key ?? ""] ?? UNKNOWN;

/** The most specific thing known: the verdict when there is one, else the coarse status. */
export const verdictKey = (submission: { verdict?: string | null; status?: string | null }): string =>
  submission.verdict ?? submission.status ?? "PENDING";

/** A test count means nothing when the code never ran or the judge itself failed. */
export const showsTestCount = (key: string): boolean => key !== "COMPILE_ERROR" && key !== "SYSTEM_ERROR";
