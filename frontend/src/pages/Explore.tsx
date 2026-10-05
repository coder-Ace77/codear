import { useState, useEffect } from "react";
import Pagination from "@/molecules/Pagination";
import { ProblemSummary } from "@/constants/mockData";
import { fetchGrandTotal ,fetchProblems } from "@/service/problemService";
import ExplorePageHeader from "@/molecules/ExplorePageHeader";
import ExplorePageMenuSection from "@/molecules/ExplorePageMenuSectiob";
import ExplorePageProblemSection from "@/molecules/ExplorePageProblemSection";

const Explore = () => {
  const [problems, setProblems] = useState<ProblemSummary[]>([]);
  const [availableTags, setAvailableTags] = useState<string[]>([]);
  const [grandTotalProblems, setGrandTotalProblems] = useState(0);


  const [totalProblems, setTotalProblems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

 
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [searchQuery, setSearchQuery] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedDifficulty, setSelectedDifficulty] = useState("all");
  const [sortBy, setSortBy] = useState("popularity");
  const [selectedTag, setSelectedTag] = useState("");

  useEffect(() => {
    setLoading(true);

    fetchProblems({
        page: currentPage,
        search: searchQuery,
        difficulty: selectedDifficulty,
        sortBy,
        tag: selectedTag,
        onSuccess: (data) => {
          setProblems(data.problems);
          setTotalPages(data.totalPages);
          setTotalProblems(data.totalCount);
          setLoading(false);
        },
        onError: (err) => {
          setError(err.message || "Failed to fetch problems");
          setLoading(false);
        },
      });
  }, [currentPage, searchQuery, selectedDifficulty, sortBy, selectedTag]);

  useEffect(() => {
    const result = fetchGrandTotal(setGrandTotalProblems , setAvailableTags);
  } , [])

  
  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, selectedDifficulty, sortBy, selectedTag]);

  return (
    <main className="mx-auto max-w-[1600px] px-6 pb-16 pt-12">
      <ExplorePageHeader grandTotalProblems={grandTotalProblems} />

      <ExplorePageMenuSection
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
        selectedDifficulty={selectedDifficulty}
        setSelectedDifficulty={setSelectedDifficulty}
        sortBy={sortBy}
        setSortBy={setSortBy}
        availableTags={availableTags}
        selectedTag={selectedTag}
        setSelectedTag={setSelectedTag}
      />

      <ExplorePageProblemSection
        loading={loading}
        problemsummary={problems}
        error={error}
      />

      {!loading && !error && (
        <p className="mt-3 font-mono text-[13px] text-muted-foreground">
          Showing {problems.length} of {totalProblems} problems
        </p>
      )}

      {!loading && !error && totalPages > 1 && (
        <div className="mt-8">
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={setCurrentPage}
          />
        </div>
      )}
    </main>
  );
};

export default Explore;
