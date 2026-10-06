import apiClient from "@/lib/apiClient";
import { Problem } from "@/types/problem";
import type { SubmissionProgress } from "@/types/submissionProgress";


export const fetchProblem = async (
    setLoading,
    setError,
    setProblem,
    id,
    ) => {
      setLoading(true);
      setError(null);
      try {
        const response = await apiClient.get<Problem>(`/problem/problem/${id}`);
        setProblem(response.data);
      } catch (err) {
        console.error("Failed to fetch problem:", err);
        setError("Failed to load problem. Please try again.");
      } finally {
        setLoading(false);
      }
    };


export const codingService = {
  async submitCode(problemId, code, language) {
    const body = { problemId, code, language };
    const response = await apiClient.post('/problem/submit', body);
    return response.data; 
  },

  async getSubmissionStatus(submissionId) {
    const response = await apiClient.get(`/problem/submissions/${submissionId}`);
    return response.data; 
  },

  /**
   * Long-poll one step of a submission's progress. The server holds the request until something newer than
   * `since` exists (or about 20 s pass), so call it in a loop with the version from the last answer.
   */
  async getSubmissionProgress(submissionId: string, since: number, signal?: AbortSignal) {
    const response = await apiClient.get<SubmissionProgress>(`/problem/submissions/${submissionId}/progress`, {
      params: { since, wait: 20 },
      signal,
      timeout: 35000, // the server waits up to 20 s, plus the network
    });
    return response.data;
  },

  async getRunStatus(submissionId) {
    const response = await apiClient.get(`/problem/submissions/test/${submissionId}`);
    return response.data; 
  },

  async runCode(problemId, code, language,input){
    const body = { problemId, code, language, input};
    const response = await apiClient.post("/problem/test", body);
    return response.data; 
  },
};
