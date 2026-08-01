import apiClient from "@/lib/apiClient";

export const aiService = {
  async chatWithAssistant(problemStatement: string, code: string, userMessage: string, problemId: string) {
    const body = {
      problemStatement,
      code,
      userMessage,
      problemId
    };
    const response = await apiClient.post('/user/chat', body);
    return response.data;
  },

  async getChatHistory(problemId: string) {
    const response = await apiClient.get(`/user/chat/history/${problemId}`);
    return response.data;
  },
};