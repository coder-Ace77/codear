import apiClient from "@/lib/apiClient";
import type { TestSummary } from "@/service/adminService";

/** No difficulty on purpose: it is not shown while a test is running. */
export interface AttemptProblem {
  id: number;
  title: string;
}

export interface ActiveAttempt {
  attemptId: number;
  testId: number;
  title: string;
  startedAt: string;
  expiresAt: string | null;
  secondsRemaining: number | null;
  solved: number;
  total: number;
}

export interface Attempt {
  attemptId: number;
  testId: number;
  title: string;
  description: string | null;
  startedAt: string;
  expiresAt: string | null;
  /** Seconds left when the response was made; null when untimed. */
  secondsRemaining: number | null;
  /** Finished by the user, or out of time. */
  finished: boolean;
  problems: AttemptProblem[];
  /** Ids of the problems already accepted in this attempt. */
  solved: number[];
}

export const testService = {
  async publicTests() {
    return (await apiClient.get<TestSummary[]>("/problem/tests/public")).data;
  },
  async startPublic(testId: number) {
    return (await apiClient.post<Attempt>(`/problem/tests/public/${testId}/start`)).data;
  },
  async startWithInvite(token: string) {
    return (await apiClient.post<Attempt>(`/problem/tests/invite/${encodeURIComponent(token)}/start`)).data;
  },
  async activeAttempts() {
    return (await apiClient.get<ActiveAttempt[]>("/problem/tests/attempts/active")).data;
  },
  async attempt(attemptId: number) {
    return (await apiClient.get<Attempt>(`/problem/tests/attempts/${attemptId}`)).data;
  },
  async finish(attemptId: number) {
    return (await apiClient.post<Attempt>(`/problem/tests/attempts/${attemptId}/finish`)).data;
  },
};

export interface ApiError {
  response?: { status?: number; data?: { detail?: unknown } };
}

export const apiErrorText = (e: unknown, fallback: string) => {
  const detail = (e as ApiError)?.response?.data?.detail;
  return typeof detail === "string" ? detail : fallback;
};
