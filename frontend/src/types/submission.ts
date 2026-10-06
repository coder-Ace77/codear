export type SubmissionStatus = "PENDING" | "RUNNING" | "PASSED" | "FAILED" | "COMPLETED" | "ERROR";
export interface Submission {
  id: number;
  submissionId: string;
  userId: number;
  problemId: number;
  code: string;
  language: string;
  status: SubmissionStatus;
  /** Why it got that status; null for submissions judged before verdicts existed. */
  verdict?: string | null;
  /** 1-based number of the first failing test, if any. */
  failedTest?: number | null;
  result: string;
  totalTests: number;
  passedTests: number;
  submittedAt: string; 
  timeTakenMs: number;
  memoryUsed: string;
  errorLog?: string;
}