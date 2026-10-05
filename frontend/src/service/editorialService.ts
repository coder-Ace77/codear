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

// The API serialises some fields by alias (created_at, is_admin, ...) and the rest as-is,
// so accept both spellings and hand the UI one consistent shape.
const normalize = (raw: any): Editorial => ({
    id: raw.id,
    problemId: raw.problemId ?? raw.problem_id,
    userId: raw.userId ?? raw.user_id,
    username: raw.username,
    title: raw.title,
    content: raw.content,
    isAdmin: Boolean(raw.isAdmin ?? raw.is_admin),
    upvotes: raw.upvotes ?? 0,
    createdAt: raw.createdAt ?? raw.created_at ?? "",
});

export const editorialService = {
    async getEditorials(problemId: number) {
        const response = await apiClient.get<any[]>(`/problem/${problemId}/editorial`);
        return response.data.map(normalize);
    },

    async submitEditorial(problemId: number, title: string, content: string) {
        const body = { problemId, title, content };
        const response = await apiClient.post<Editorial>(`/problem/${problemId}/editorial`, body);
        return response.data;
    }
};
