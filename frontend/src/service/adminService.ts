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

export type AssessmentVisibility = "PUBLIC" | "PRIVATE";
export type SlotDifficulty = "ANY" | "EASY" | "MEDIUM" | "HARD";
export type AssessmentSelectionMode = "ALL_PROBLEMS" | "POOL";

export interface CreateAssessmentInput {
  title: string;
  description?: string;
  visibility: AssessmentVisibility;
  selectionMode: AssessmentSelectionMode;
  poolProblemIds?: number[];
  problemCount: number;
  /** One entry per problem, in order. Omit for no preference. */
  slotDifficulties?: SlotDifficulty[];
  /** Omit for an untimed assessment. */
  durationMinutes?: number;
  /** false: one attempt per user. */
  multipleAttempts: boolean;
  /** singleUse left out: follow multipleAttempts. */
  invites: { usernames: string[]; singleUse?: boolean; expiresAt?: string };
}

export interface AssessmentInvite {
  id: number;
  userId: number;
  username: string | null;
  token: string;
  singleUse: boolean;
  expiresAt: string | null;
  revokedAt: string | null;
  /** Path of the link on this site, e.g. /assessment/<token>. */
  path: string;
  used: boolean;
}

export interface AssessmentSummary {
  id: number;
  title: string;
  description: string | null;
  visibility: AssessmentVisibility;
  selectionMode: AssessmentSelectionMode;
  problemCount: number;
  slotDifficulties: SlotDifficulty[] | null;
  durationMinutes: number | null;
  multipleAttempts: boolean;
  isActive: boolean;
  createdAt: string | null;
  attempts: number;
  invites: number;
}

export interface AssessmentDetail extends AssessmentSummary {
  poolProblemIds: number[] | null;
  inviteList: AssessmentInvite[];
}

export interface CreatedAssessment {
  assessment: AssessmentDetail;
  /** Usernames that matched nobody. */
  unknownUsernames: string[];
}

export interface AttemptResult {
  attemptId: number;
  assessmentId: number;
  assessmentTitle: string;
  userId: number;
  username: string | null;
  startedAt: string;
  finishedAt: string | null;
  expiresAt: string | null;
  /** The assessment's time limit; null when untimed. */
  durationMinutes: number | null;
  over: boolean;
  outcome: "FINISHED" | "TIME_UP" | "IN_PROGRESS";
  /** Start to finish (or to the deadline); so far, for a running attempt. */
  timeTakenSeconds: number;
  solvedCount: number;
  total: number;
  problems: { id: number; title: string; solved: boolean; submissions: number; solvedAt: string | null }[];
}

/** One user's completed assessment attempts, newest first, with totals. */
export interface AssessmentHistory {
  userId: number;
  username: string | null;
  attemptsTaken: number;
  problemsSolved: number;
  problemsGiven: number;
  averageTimeSeconds: number | null;
  attempts: AttemptResult[];
}

export const assessmentAdminService = {
  async history(username: string) {
    return (await apiClient.get<AssessmentHistory>("/problem/admin/assessments/history", { params: { username } })).data;
  },
  async results(id: number) {
    return (await apiClient.get<AttemptResult[]>(`/problem/admin/assessments/${id}/results`)).data;
  },
  async list() {
    return (await apiClient.get<AssessmentSummary[]>("/problem/admin/assessments")).data;
  },
  async get(id: number) {
    return (await apiClient.get<AssessmentDetail>(`/problem/admin/assessments/${id}`)).data;
  },
  async create(input: CreateAssessmentInput) {
    return (await apiClient.post<CreatedAssessment>("/problem/admin/assessments", input)).data;
  },
  async addInvites(id: number, invites: CreateAssessmentInput["invites"]) {
    return (await apiClient.post<CreatedAssessment>(`/problem/admin/assessments/${id}/invites`, invites)).data;
  },
  async setActive(id: number, isActive: boolean) {
    return (await apiClient.patch<AssessmentDetail>(`/problem/admin/assessments/${id}`, { isActive })).data;
  },
  async revokeInvite(assessmentId: number, inviteId: number) {
    await apiClient.delete(`/problem/admin/assessments/${assessmentId}/invites/${inviteId}`);
  },
  async remove(id: number) {
    await apiClient.delete(`/problem/admin/assessments/${id}`);
  },
};
