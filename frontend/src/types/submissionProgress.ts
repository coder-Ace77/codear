import type { Submission } from "@/types/submission";

export type ProgressStage = "QUEUED" | "PREPARING" | "RUNNING" | "JUDGING" | "DONE" | "ERROR";

/** What GET /problem/submissions/{id}/progress returns. */
export interface SubmissionProgress {
  submissionId: string;
  /** Only ever increases; pass the last one back as `since` to wait for something newer. */
  version: number;
  stage: ProgressStage;
  /** Human wording from the server, e.g. "Running test 7 of 26". */
  message: string;
  /** 0-100 on one scale for every client. */
  percent: number;
  total: number | null;
  /** Number of the test currently running (0 while compiling or starting). */
  started: number;
  completed: number;
  terminal: boolean;
  /** False when the wait timed out with nothing new. */
  changed: boolean;
  status: "IN_PROGRESS" | "PASSED" | "FAILED" | null;
  /** Present once terminal: the full verdict. */
  result?: Submission | null;
}
