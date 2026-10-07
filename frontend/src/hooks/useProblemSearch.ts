import { useEffect, useState } from "react";
import axios from "axios";
import { fetchTagCounts, searchProblems } from "@/service/problemService";
import type { ProblemQuery, SearchResult, TagCount } from "@/types/problemSearch";

const EMPTY: SearchResult = { problems: [], totalCount: 0, totalPages: 0 };

/**
 * Loads the problems for a query. A newer query cancels the one still in flight, so a slow answer to an old search
 * can never overwrite the answer to the current one, and the previous list stays on screen while the next loads.
 */
export const useProblemSearch = (query: ProblemQuery) => {
  const [result, setResult] = useState<SearchResult>(EMPTY);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // the query is an object that is rebuilt on every render, so depend on what it says
  const key = JSON.stringify(query);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    searchProblems(JSON.parse(key) as ProblemQuery, controller.signal)
      .then((found) => {
        setResult(found);
        setError(null);
        setLoading(false);
      })
      .catch((e) => {
        if (axios.isCancel(e)) return;
        setError(e.response?.data?.detail ?? "Could not load the problems.");
        setLoading(false);
      });
    return () => controller.abort();
  }, [key]);

  return { ...result, loading, error };
};

/** Every tag with how many problems carry it. Loaded once. */
export const useTagCounts = () => {
  const [tags, setTags] = useState<TagCount[]>([]);

  useEffect(() => {
    let alive = true;
    fetchTagCounts()
      .then((counts) => alive && setTags(counts))
      .catch(() => alive && setTags([]));
    return () => {
      alive = false;
    };
  }, []);

  return tags;
};
