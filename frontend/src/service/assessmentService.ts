import apiClient from "@/lib/apiClient";
import type { AssessmentHistory, AssessmentSummary } from "@/service/adminService";

/** No difficulty on purpose: it is not shown while an assessment is running. */
export interface AttemptProblem {
  id: number;
  title: string;
}

export interface ActiveAttempt {
  attemptId: number;
  assessmentId: number;
  title: string;
  startedAt: string;
  expiresAt: string | null;
  secondsRemaining: number | null;
  solved: number;
  total: number;
}

export interface Attempt {
  attemptId: number;
  assessmentId: number;
  title: string;
  description: string | null;
  startedAt: string;
  expiresAt: string | null;
  /** Seconds left when the response was made; null when untimed. */
  secondsRemaining: number | null;
  /** Seconds since it started, stopping when it ended. */
  secondsElapsed: number;
  /** Finished by the user, or out of time. */
  finished: boolean;
  problems: AttemptProblem[];
  /** Ids of the problems already accepted in this attempt. */
  solved: number[];
}

/** A public assessment, with where the caller stands on it. */
export interface PublicAssessment extends AssessmentSummary {
  myStatus: "NOT_STARTED" | "IN_PROGRESS" | "DONE" | "RETAKE";
  myAttemptId: number | null;
}

export const assessmentService = {
  async publicAssessments() {
    return (await apiClient.get<PublicAssessment[]>("/problem/assessments/public")).data;
  },
  async startPublic(assessmentId: number) {
    return (await apiClient.post<Attempt>(`/problem/assessments/public/${assessmentId}/start`)).data;
  },
  async startWithInvite(token: string) {
    return (await apiClient.post<Attempt>(`/problem/assessments/invite/${encodeURIComponent(token)}/start`)).data;
  },
  async history() {
    return (await apiClient.get<AssessmentHistory>("/problem/assessments/history")).data;
  },
  async activeAttempts() {
    return (await apiClient.get<ActiveAttempt[]>("/problem/assessments/attempts/active")).data;
  },
  async attempt(attemptId: number) {
    return (await apiClient.get<Attempt>(`/problem/assessments/attempts/${attemptId}`)).data;
  },
  async finish(attemptId: number) {
    return (await apiClient.post<Attempt>(`/problem/assessments/attempts/${attemptId}/finish`)).data;
  },
};

export interface ApiError {
  response?: { status?: number; data?: { detail?: unknown } };
}

export const apiErrorText = (e: unknown, fallback: string) => {
  const detail = (e as ApiError)?.response?.data?.detail;
  return typeof detail === "string" ? detail : fallback;
};
