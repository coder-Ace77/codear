import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { SEARCH_DEBOUNCE_MS } from "@/constants/problemFilters";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { DEFAULT_QUERY, hasFilters, queryFromUrl, urlFromQuery } from "@/lib/problemQueryUrl";
import type { ProblemQuery } from "@/types/problemSearch";

/**
 * The problems page's filters as state that lives in the address. Changing a filter goes back to the first page, and
 * the search text is typed into a local box and only reaches the address (and so the API) once typing pauses.
 */
export const useProblemQuery = () => {
  const [params, setParams] = useSearchParams();
  const query = useMemo(() => queryFromUrl(params), [params]);

  const update = useCallback(
    (changes: Partial<ProblemQuery>, { keepPage = false } = {}) => {
      const next = { ...query, ...changes, page: keepPage ? (changes.page ?? query.page) : 1 };
      // replace, so typing and clicking filters does not fill the back button with every step
      setParams(urlFromQuery(next), { replace: true });
    },
    [query, setParams]
  );

  // ---- the search box ----
  const [searchText, setSearchText] = useState(query.search);
  const debouncedText = useDebouncedValue(searchText, SEARCH_DEBOUNCE_MS);

  useEffect(() => {
    if (debouncedText.trim() !== query.search) update({ search: debouncedText.trim() });
    // only react to the pause in typing, not to every change of the address
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedText]);

  // keep the box in step when the address changes by itself (back button, a link, "clear filters")
  useEffect(() => {
    setSearchText((current) => (current.trim() === query.search ? current : query.search));
  }, [query.search]);

  const toggleTag = (tag: string) => {
    const lower = tag.toLowerCase();
    update({ tags: query.tags.includes(lower) ? query.tags.filter((t) => t !== lower) : [...query.tags, lower] });
  };

  return {
    query,
    searchText,
    setSearchText,
    setDifficulty: (difficulty: string) => update({ difficulty }),
    toggleTag,
    removeTag: (tag: string) => update({ tags: query.tags.filter((t) => t !== tag.toLowerCase()) }),
    setTagMode: (tagMode: string) => update({ tagMode }),
    setSort: (sort: string) => update({ sort }),
    setPage: (page: number) => update({ page }, { keepPage: true }),
    clearSearch: () => {
      setSearchText("");
      update({ search: "" });
    },
    clearFilters: () => {
      setSearchText("");
      setParams(urlFromQuery({ ...DEFAULT_QUERY, sort: query.sort }), { replace: true });
    },
    filtered: hasFilters(query),
  };
};
