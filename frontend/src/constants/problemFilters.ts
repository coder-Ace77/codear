/** The choices on the problems page. The values are the API's: see the backend's core/search_options.py. */

export interface Option {
  value: string;
  label: string;
}

export const DIFFICULTY_OPTIONS: Option[] = [
  { value: "all", label: "All" },
  { value: "easy", label: "Easy" },
  { value: "medium", label: "Medium" },
  { value: "hard", label: "Hard" },
];

export const TAG_MODES: Option[] = [
  { value: "any", label: "Any of these" },
  { value: "all", label: "All of these" },
];

export const SORT_RELEVANCE = "relevance";

/** "Best match" only means something while searching; otherwise the same choice is the default order. */
export const sortOptions = (searching: boolean): Option[] => [
  { value: SORT_RELEVANCE, label: searching ? "Best match" : "Default order" },
  { value: "popularity", label: "Most solved" },
  { value: "acceptance", label: "Highest acceptance" },
  { value: "newest", label: "Newest" },
  { value: "oldest", label: "Oldest" },
  { value: "title", label: "Title A to Z" },
  { value: "easiest", label: "Easiest first" },
  { value: "hardest", label: "Hardest first" },
];

export const DEFAULT_DIFFICULTY = "all";
export const DEFAULT_TAG_MODE = "any";
export const SEARCH_DEBOUNCE_MS = 300;
export const SEARCH_MAX_LENGTH = 100;
/** Tags shown before "Show all tags". */
export const VISIBLE_TAGS = 10;
