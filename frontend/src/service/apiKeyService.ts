import apiClient from "@/lib/apiClient";

export interface ApiKey {
  id: number;
  name: string;
  prefix: string;
  createdAt: string;
  lastUsedAt: string | null;
}

export interface CreatedApiKey extends ApiKey {
  /** The full key. Returned once, at creation, and never again. */
  key: string;
}

export const apiKeyService = {
  async list() {
    const res = await apiClient.get<ApiKey[]>("/user/api-keys");
    return res.data;
  },

  async create(name: string) {
    const res = await apiClient.post<CreatedApiKey>("/user/api-keys", { name });
    return res.data;
  },

  async revoke(id: number) {
    await apiClient.delete(`/user/api-keys/${id}`);
  },
};
