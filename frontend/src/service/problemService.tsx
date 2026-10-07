import { ITEMS_PER_PAGE } from "@/constants/AppConstants";
import { DEFAULT_DIFFICULTY, DEFAULT_TAG_MODE } from "@/constants/problemFilters";
import apiClient from "@/lib/apiClient";
import type { ProblemQuery, SearchResult, TagCount } from "@/types/problemSearch";
import type { TagsAndCount } from "@/types/TagsAndCount";

/**
 * Query-string parameters for the API. Axios writes an array as `tags[]=a`, but the API reads `tags=a&tags=b`,
 * so the list is spelled out here.
 */
export const searchParams = (query: ProblemQuery): URLSearchParams => {
  const params = new URLSearchParams();
  if (query.search.trim()) params.set("search", query.search.trim());
  if (query.difficulty !== DEFAULT_DIFFICULTY) params.set("difficulty", query.difficulty);
  query.tags.forEach((tag) => params.append("tags", tag));
  if (query.tags.length > 1 && query.tagMode !== DEFAULT_TAG_MODE) params.set("tagMode", query.tagMode);
  params.set("sortBy", query.sort);
  params.set("page", String(Math.max(query.page, 1) - 1)); // the API counts pages from 0
  params.set("size", String(query.size ?? ITEMS_PER_PAGE));
  return params;
};

export const searchProblems = async (query: ProblemQuery, signal?: AbortSignal): Promise<SearchResult> => {
  const { data } = await apiClient.get("/problem/search", { params: searchParams(query), signal });
  return {
    problems: data.content ?? [],
    totalCount: data.totalCount ?? 0,
    totalPages: data.totalPages ?? 0,
  };
};

export const fetchTagCounts = async (): Promise<TagCount[]> => {
  const { data } = await apiClient.get<TagCount[]>("/problem/tags");
  return data;
};

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

export const deleteProblem = async (id: number) => {
  try {
    await apiClient.delete(`/problem/${id}`);
    return true;
  } catch (error) {
    console.error("Failed to delete problem:", error);
    throw error;
  }
};
