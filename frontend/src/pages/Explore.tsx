import { useState, useEffect } from "react";
import Pagination from "@/molecules/Pagination";
import { ProblemSummary } from "@/constants/mockData";
import { fetchGrandTotal ,fetchProblems } from "@/service/problemService";
import ExplorePageHeader from "@/molecules/ExplorePageHeader";
import ExplorePageMenuSection from "@/molecules/ExplorePageMenuSectiob";
import ExplorePageProblemSection from "@/molecules/ExplorePageProblemSection";
import FloatingShapes from "@/molecules/FloatingShapes";

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
    <div className="relative min-h-screen py-10 md:py-14 px-6 md:px-10 overflow-hidden">
      <FloatingShapes />
      <div className="relative z-10 max-w-[1600px] mx-auto">
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
        <div className="mb-4 font-['Space_Mono'] text-[10px] tracking-[0.25em] uppercase text-muted-foreground">
          Showing {problems.length} / {totalProblems} problems
        </div>

        <ExplorePageProblemSection
          loading={loading}
          problemsummary={problems}
          error={error}
        />
        {!loading && !error && totalPages > 1 && (
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={setCurrentPage}
          />
        )}
      </div>
    </div>
  );
};

export default Explore;
