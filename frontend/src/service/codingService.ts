import apiClient from "@/lib/apiClient";
import { Problem } from "@/types/problem";


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
    const body = { problemId, code, language, userId:1};
    console.log(code)
    const response = await apiClient.post('/problem/submit', body);
    return response.data; 
  },

  async getSubmissionStatus(submissionId) {
    const response = await apiClient.get(`/problem/submissions/${submissionId}`);
    return response.data; 
  },

  async getRunStatus(submissionId) {
    const response = await apiClient.get(`/problem/submissions/test/${submissionId}`);
    return response.data; 
  },

  async runCode(problemId, code, language,input){
    const body = { problemId, code, language, userId:1,input};
    const response = await apiClient.post("/problem/test", body);
    return response.data; 
  },
};
