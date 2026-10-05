import SearchBar from "./SearchBar";
import Select from "@/atoms/Select";

const label = "overline mb-1 block";

const ExplorePageMenuSection = ({
  searchQuery,
  setSearchQuery,
  selectedDifficulty,
  setSelectedDifficulty,
  sortBy,
  setSortBy,
  setSelectedTag,
  availableTags,
  selectedTag,
}) => {
  return (
    <div className="mb-4 space-y-4">
      <div className="grid gap-4 md:grid-cols-[1fr_auto_auto] md:items-end">
        <div>
          <span className={label}>Search</span>
          <SearchBar
            value={searchQuery}
            onChange={setSearchQuery}
            placeholder="Search problems by title"
          />
        </div>

        <div>
          <span className={label}>Difficulty</span>
          <Select
            value={selectedDifficulty}
            onChange={(e) => setSelectedDifficulty(e.target.value)}
            className="md:w-44"
            aria-label="Difficulty"
          >
            <option value="all">All difficulties</option>
            <option value="easy">Easy</option>
            <option value="medium">Medium</option>
            <option value="hard">Hard</option>
          </Select>
        </div>

        <div>
          <span className={label}>Sort by</span>
          <Select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="md:w-44"
            aria-label="Sort by"
          >
            <option value="popularity">Most popular</option>
            <option value="acceptance">Acceptance rate</option>
            <option value="latest">Latest</option>
          </Select>
        </div>
      </div>

      <div className="flex flex-wrap gap-2" role="group" aria-label="Filter by topic">
        <TagChip active={!selectedTag} onClick={() => setSelectedTag("")}>
          All topics
        </TagChip>
        {availableTags.slice(0, 12).map((tag) => (
          <TagChip
            key={tag}
            active={selectedTag === tag}
            onClick={() => setSelectedTag(tag === selectedTag ? "" : tag)}
          >
            {tag}
          </TagChip>
        ))}
      </div>
    </div>
  );
};

const TagChip = ({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) => (
  <button
    onClick={onClick}
    aria-pressed={active}
    className={`inline-flex h-6 items-center whitespace-nowrap rounded-full border px-2 text-xs transition-colors ${
      active
        ? "border-foreground bg-foreground text-background"
        : "border-border text-muted-foreground hover:bg-highlight-wash hover:text-foreground"
    }`}
  >
    {children}
  </button>
);

export default ExplorePageMenuSection;
