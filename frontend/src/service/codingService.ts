import apiClient from "@/lib/apiClient";
import { Problem } from "@/types/problem";
import type { Submission } from "@/types/submission";


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
  async submitCode(problemId, code, language, attemptId = null) {
    const body = { problemId, code, language, ...(attemptId ? { attemptId } : {}) };
    const response = await apiClient.post('/problem/submit', body);
    return response.data; 
  },

  /** Long-polls one submission: the server holds the request for up to 10 s while it is still being judged. */
  async getSubmissionStatus(submissionId: string, signal?: AbortSignal) {
    const response = await apiClient.get<Submission>(`/problem/submissions/${submissionId}`, {
      signal,
      timeout: 25000, // the server waits up to 10 s, plus the network
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
