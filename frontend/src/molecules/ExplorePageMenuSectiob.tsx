import SearchBar from "./SearchBar";
import Select from "@/atoms/Select";

const mono = "font-['Space_Mono']";
const label = `${mono} text-[10px] tracking-[0.3em] uppercase text-muted-foreground mb-2 block`;

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
    <div className="mb-8 space-y-6">
      <div className="grid md:grid-cols-[1fr_auto_auto] gap-4 md:items-end">
        <div>
          <span className={label}>Search</span>
          <SearchBar
            value={searchQuery}
            onChange={setSearchQuery}
            placeholder="Search problems by title..."
          />
        </div>

        <div>
          <span className={label}>Difficulty</span>
          <Select
            value={selectedDifficulty}
            onChange={(e) => setSelectedDifficulty(e.target.value)}
            className="md:w-44"
          >
            <option value="all">All Difficulties</option>
            <option value="easy">Easy</option>
            <option value="medium">Medium</option>
            <option value="hard">Hard</option>
          </Select>
        </div>

        <div>
          <span className={label}>Sort By</span>
          <Select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="md:w-44"
          >
            <option value="popularity">Most Popular</option>
            <option value="acceptance">Acceptance Rate</option>
            <option value="latest">Latest</option>
          </Select>
        </div>
      </div>

      <div>
        <span className={label}>Filter by Tag</span>
        <div className="flex flex-wrap gap-2">
          <TagChip active={!selectedTag} color="hsl(var(--accent))" onClick={() => setSelectedTag("")}>
            All
          </TagChip>
          {availableTags.slice(0, 12).map((tag, i) => (
            <TagChip
              key={tag}
              active={selectedTag === tag}
              color={tagColors[i % tagColors.length]}
              onClick={() => setSelectedTag(tag === selectedTag ? "" : tag)}
            >
              {tag}
            </TagChip>
          ))}
        </div>
      </div>
    </div>
  );
};

const tagColors = [
  "hsl(var(--brand-orange))",
  "hsl(var(--brand-yellow))",
  "hsl(var(--brand-olive))",
  "hsl(var(--brand-rust))",
  "hsl(var(--brand-clay))",
  "hsl(var(--accent))",
];

const TagChip = ({
  active,
  color,
  onClick,
  children,
}: {
  active: boolean;
  color: string;
  onClick: () => void;
  children: React.ReactNode;
}) => (
  <button
    onClick={onClick}
    className={`${mono} text-[10px] tracking-[0.15em] uppercase rounded-full px-4 py-1.5 border transition-all duration-200 hover:-translate-y-0.5`}
    style={
      active
        ? { backgroundColor: color, borderColor: color, color: "hsl(var(--accent-foreground))" }
        : { borderColor: `${color}`, color, backgroundColor: "transparent" }
    }
  >
    {children}
  </button>
);

export default ExplorePageMenuSection;
