import {
  DEFAULT_DIFFICULTY,
  DEFAULT_TAG_MODE,
  DIFFICULTY_OPTIONS,
  SEARCH_MAX_LENGTH,
  SORT_RELEVANCE,
  TAG_MODES,
  sortOptions,
} from "@/constants/problemFilters";
import type { ProblemQuery } from "@/types/problemSearch";

/**
 * The filters live in the address (?q=graph&difficulty=easy&tag=string&sort=title&page=2), so a link, a reload or
 * the back button restores exactly what was on screen. These two functions are the only place that format is known.
 */

export const DEFAULT_QUERY: ProblemQuery = {
  search: "",
  difficulty: DEFAULT_DIFFICULTY,
  tags: [],
  tagMode: DEFAULT_TAG_MODE,
  sort: SORT_RELEVANCE,
  page: 1,
};

const oneOf = (value: string | null, allowed: string[], fallback: string) =>
  value && allowed.includes(value) ? value : fallback;

/** Anything missing or invalid in the address falls back to the default, so a hand-edited link never breaks the page. */
export const queryFromUrl = (params: URLSearchParams): ProblemQuery => {
  const tags = Array.from(new Set(params.getAll("tag").map((t) => t.trim().toLowerCase()).filter(Boolean)));
  const page = Number.parseInt(params.get("page") ?? "1", 10);
  return {
    search: (params.get("q") ?? "").slice(0, SEARCH_MAX_LENGTH),
    difficulty: oneOf(params.get("difficulty")?.toLowerCase() ?? null, DIFFICULTY_OPTIONS.map((o) => o.value), DEFAULT_DIFFICULTY),
    tags: tags.slice(0, 10),
    tagMode: oneOf(params.get("match"), TAG_MODES.map((o) => o.value), DEFAULT_TAG_MODE),
    sort: oneOf(params.get("sort"), sortOptions(true).map((o) => o.value), SORT_RELEVANCE),
    page: Number.isFinite(page) && page > 0 ? page : 1,
  };
};

/** Only what differs from the default is written, so the plain page keeps a clean address. */
export const urlFromQuery = (query: ProblemQuery): URLSearchParams => {
  const params = new URLSearchParams();
  if (query.search) params.set("q", query.search);
  if (query.difficulty !== DEFAULT_DIFFICULTY) params.set("difficulty", query.difficulty);
  query.tags.forEach((tag) => params.append("tag", tag));
  if (query.tags.length > 1 && query.tagMode !== DEFAULT_TAG_MODE) params.set("match", query.tagMode);
  if (query.sort !== SORT_RELEVANCE) params.set("sort", query.sort);
  if (query.page > 1) params.set("page", String(query.page));
  return params;
};

export const hasFilters = (query: ProblemQuery): boolean =>
  Boolean(query.search) || query.difficulty !== DEFAULT_DIFFICULTY || query.tags.length > 0;
