import apiClient from "@/lib/apiClient";

export interface Editorial {
    id: number;
    problemId: number;
    userId: number;
    username: string;
    title: string;
    content: string;
    isAdmin: boolean;
    upvotes: number;
    createdAt: string;
}

export const editorialService = {
    async getEditorials(problemId: number) {
        const response = await apiClient.get<Editorial[]>(`/problem/${problemId}/editorial`);
        return response.data;
    },

    async submitEditorial(problemId: number, title: string, content: string) {
        const body = { problemId, title, content };
        const response = await apiClient.post<Editorial>(`/problem/${problemId}/editorial`, body);
        return response.data;
    }
};
