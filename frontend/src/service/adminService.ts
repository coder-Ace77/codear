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

export type TestVisibility = "PUBLIC" | "PRIVATE";
export type SlotDifficulty = "ANY" | "EASY" | "MEDIUM" | "HARD";
export type TestSelectionMode = "ALL_PROBLEMS" | "POOL";

export interface CreateTestInput {
  title: string;
  description?: string;
  visibility: TestVisibility;
  selectionMode: TestSelectionMode;
  poolProblemIds?: number[];
  problemCount: number;
  /** One entry per problem, in order. Omit for no preference. */
  slotDifficulties?: SlotDifficulty[];
  /** Omit for an untimed test. */
  durationMinutes?: number;
  /** false: one attempt per user. */
  multipleAttempts: boolean;
  /** singleUse left out: follow multipleAttempts. */
  invites: { usernames: string[]; singleUse?: boolean; expiresAt?: string };
}

export interface TestInvite {
  id: number;
  userId: number;
  username: string | null;
  token: string;
  singleUse: boolean;
  expiresAt: string | null;
  revokedAt: string | null;
  /** Path of the link on this site, e.g. /test/<token>. */
  path: string;
  used: boolean;
}

export interface TestSummary {
  id: number;
  title: string;
  description: string | null;
  visibility: TestVisibility;
  selectionMode: TestSelectionMode;
  problemCount: number;
  slotDifficulties: SlotDifficulty[] | null;
  durationMinutes: number | null;
  multipleAttempts: boolean;
  isActive: boolean;
  createdAt: string | null;
  attempts: number;
  invites: number;
}

export interface TestDetail extends TestSummary {
  poolProblemIds: number[] | null;
  inviteList: TestInvite[];
}

export interface CreatedTest {
  test: TestDetail;
  /** Usernames that matched nobody. */
  unknownUsernames: string[];
}

export interface AttemptResult {
  attemptId: number;
  userId: number;
  username: string | null;
  startedAt: string;
  finishedAt: string | null;
  expiresAt: string | null;
  over: boolean;
  solvedCount: number;
  total: number;
  problems: { id: number; title: string; solved: boolean; submissions: number; solvedAt: string | null }[];
}

export const testAdminService = {
  async results(id: number) {
    return (await apiClient.get<AttemptResult[]>(`/problem/admin/tests/${id}/results`)).data;
  },
  async list() {
    return (await apiClient.get<TestSummary[]>("/problem/admin/tests")).data;
  },
  async get(id: number) {
    return (await apiClient.get<TestDetail>(`/problem/admin/tests/${id}`)).data;
  },
  async create(input: CreateTestInput) {
    return (await apiClient.post<CreatedTest>("/problem/admin/tests", input)).data;
  },
  async addInvites(id: number, invites: CreateTestInput["invites"]) {
    return (await apiClient.post<CreatedTest>(`/problem/admin/tests/${id}/invites`, invites)).data;
  },
  async setActive(id: number, isActive: boolean) {
    return (await apiClient.patch<TestDetail>(`/problem/admin/tests/${id}`, { isActive })).data;
  },
  async revokeInvite(testId: number, inviteId: number) {
    await apiClient.delete(`/problem/admin/tests/${testId}/invites/${inviteId}`);
  },
  async remove(id: number) {
    await apiClient.delete(`/problem/admin/tests/${id}`);
  },
};
