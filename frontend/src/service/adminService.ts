import apiClient from "@/lib/apiClient";

export interface QueueDepth {
  name: string;
  configured: boolean;
  waiting?: number;
  inFlight?: number;
  delayed?: number;
  error?: string;
}

export interface AdminStats {
  generatedAt: string;
  queues: QueueDepth[];
  submissions: {
    total: number;
    lastMinute: number;
    last5Minutes: number;
    lastHour: number;
    last24Hours: number;
    perSecondLastMinute: number;
    inProgress: number;
    oldestInProgressSeconds: number | null;
    stuck: boolean;
    byStatusLast24Hours: Record<string, number>;
    byVerdictLast24Hours?: Record<string, number>;
    avgJudgeMsLastHour: number | null;
  };
  topSubmittersLastHour: { userId: number; username: string; submissions: number }[];
  rateLimit: { blockedLast5Minutes: number };
}

export interface RecentSubmission {
  submissionId: string;
  userId: number;
  username: string;
  problemId: number;
  problemTitle: string | null;
  language: string;
  status: "IN_PROGRESS" | "PASSED" | "FAILED" | "COMPLETED" | null;
  verdict?: string | null;
  failedTest?: number | null;
  passedTests: number | null;
  totalTests: number | null;
  timeTakenMs: number | null;
  submittedAt: string | null;
  ageSeconds: number | null;
}

export const adminService = {
  async stats() {
    const res = await apiClient.get<AdminStats>("/problem/admin/stats");
    return res.data;
  },

  async recentSubmissions(limit = 30) {
    const res = await apiClient.get<RecentSubmission[]>("/problem/admin/submissions", { params: { limit } });
    return res.data;
  },
};
