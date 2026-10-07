import { useEffect } from "react";
import ExplorePageHeader from "@/molecules/ExplorePageHeader";
import ExplorePageProblemSection from "@/molecules/ExplorePageProblemSection";
import Pagination from "@/molecules/Pagination";
import ProblemFilters from "@/molecules/ProblemFilters";
import { ITEMS_PER_PAGE } from "@/constants/AppConstants";
import { useProblemQuery } from "@/hooks/useProblemQuery";
import { useProblemSearch, useTagCounts } from "@/hooks/useProblemSearch";

const Explore = () => {
  const filters = useProblemQuery();
  const tags = useTagCounts();
  const { problems, totalCount, totalPages, loading, error } = useProblemSearch(filters.query);

  // an address can name a page that no longer exists (a link from before the list shrank): go to the last real one
  const { page } = filters.query;
  const { setPage } = filters;
  useEffect(() => {
    if (!loading && !error && problems.length === 0 && totalCount > 0 && page > 1) setPage(Math.max(totalPages, 1));
  }, [loading, error, problems.length, totalCount, totalPages, page, setPage]);

  const first = (filters.query.page - 1) * ITEMS_PER_PAGE + 1;
  const last = first + problems.length - 1;

  return (
    <main className="mx-auto max-w-[1600px] px-6 pb-16 pt-12">
      <ExplorePageHeader grandTotalProblems={totalCount} filtered={filters.filtered} />

      <ProblemFilters
        query={filters.query}
        searchText={filters.searchText}
        tags={tags}
        onSearchText={filters.setSearchText}
        onClearSearch={filters.clearSearch}
        onDifficulty={filters.setDifficulty}
        onToggleTag={filters.toggleTag}
        onRemoveTag={filters.removeTag}
        onTagMode={filters.setTagMode}
        onSort={filters.setSort}
        onClearAll={filters.clearFilters}
      />

      <ExplorePageProblemSection
        loading={loading}
        error={error}
        problems={problems}
        selectedTags={filters.query.tags}
        filtered={filters.filtered}
        onTagClick={filters.toggleTag}
        onClearFilters={filters.clearFilters}
      />

      {!error && totalCount > 0 && (
        <p className="mt-3 font-mono text-[13px] text-muted-foreground" aria-live="polite">
          Showing {first}–{last} of {totalCount} {totalCount === 1 ? "problem" : "problems"}
        </p>
      )}

      {!error && totalPages > 1 && (
        <div className="mt-8">
          <Pagination currentPage={filters.query.page} totalPages={totalPages} onPageChange={filters.setPage} />
        </div>
      )}
    </main>
  );
};

export default Explore;
