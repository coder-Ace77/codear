export interface ProblemSummary {
  id: number;
  title: string;
  difficulty: "Easy" | "Medium" | "Hard";
  tags: string[];
  /** Percent of submissions that were accepted, or null while nobody has submitted. */
  acceptance: number | null;
  submissions: number;
  solvedBy: number;
}

/** Everything that decides which problems are listed, and in what order. */
export interface ProblemQuery {
  search: string;
  difficulty: string;
  /** Lower-case tags. */
  tags: string[];
  tagMode: string;
  sort: string;
  /** From 1. */
  page: number;
  size?: number;
}

export interface SearchResult {
  problems: ProblemSummary[];
  totalCount: number;
  totalPages: number;
}

export interface TagCount {
  tag: string;
  count: number;
}
