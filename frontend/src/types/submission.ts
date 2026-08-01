export type SubmissionStatus = "PENDING" | "RUNNING" | "PASSED" | "FAILED" | "COMPLETED" | "ERROR";
export interface Submission {
  id: number;
  submissionId: string;
  userId: number;
  problemId: number;
  code: string;
  language: string;
  status: SubmissionStatus;
  result: string;
  totalTests: number;
  passedTests: number;
  submittedAt: string; 
  timeTakenMs: number;
  memoryUsed: string;
  errorLog?: string;
}