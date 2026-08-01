import { ApiResponse } from "@/types/apiResponse";
import { ITEMS_PER_PAGE } from "@/constants/AppConstants";
import apiClient from "@/lib/apiClient";
import type { TagsAndCount } from "@/types/TagsAndCount";
import { ProblemSummary } from "@/constants/mockData";

export const fetchGrandTotal = async (setGrandTotalProblems, setAvailableTags) => {
  try {
    const response = await apiClient("/problem/problemCntAndTags");
    const data: TagsAndCount = await response.data;
    setGrandTotalProblems(data.count || 0);
    setAvailableTags(data.tags || []);
  } catch (err) {
    console.error("Failed to fetch grand total:", err);
    setGrandTotalProblems(0);
  }
};

export const fetchProblems = async ({
  page,
  search,
  difficulty,
  sortBy,
  tag,
  onSuccess,
  onError,
}: {
  page: number;
  search: string;
  difficulty: string;
  sortBy: string;
  tag: string;
  onSuccess: (data: any) => void;
  onError: (err: any) => void;
}) => {
  try {
    console.log("fetch Problem called..");
    const params: any = {
      page: page - 1,
      size: ITEMS_PER_PAGE,
      sortBy,
    };

    if (search) params.search = search;
    if (difficulty !== "all") params.difficulty = difficulty;
    if (tag) params.tags = [tag];

    console.log("params", params);

    const { data } = await apiClient(`problem/search`, { params });

    console.log("data ", data);

    onSuccess({
      problems: data.content || [],
      totalCount: data.totalCount || 0,
      totalPages: data.totalPages || 1,
    });
  } catch (err: any) {
    console.error("❌ fetchProblems failed:", err);
    onError(err);
  }
};



export const deleteProblem = async (id: number) => {
  try {
    await apiClient.delete(`/problem/${id}`);
    return true;
  } catch (error) {
    console.error("Failed to delete problem:", error);
    throw error;
  }
};
