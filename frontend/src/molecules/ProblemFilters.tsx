import Select from "@/atoms/Select";
import { DEFAULT_DIFFICULTY, sortOptions } from "@/constants/problemFilters";
import type { ProblemQuery, TagCount } from "@/types/problemSearch";
import ActiveFilters from "@/molecules/filters/ActiveFilters";
import DifficultyFilter from "@/molecules/filters/DifficultyFilter";
import SearchBox from "@/molecules/filters/SearchBox";
import TagFilter from "@/molecules/filters/TagFilter";

interface ProblemFiltersProps {
  query: ProblemQuery;
  searchText: string;
  tags: TagCount[];
  onSearchText: (value: string) => void;
  onClearSearch: () => void;
  onDifficulty: (value: string) => void;
  onToggleTag: (tag: string) => void;
  onRemoveTag: (tag: string) => void;
  onTagMode: (mode: string) => void;
  onSort: (sort: string) => void;
  onClearAll: () => void;
}

/** Everything that narrows or orders the problems list. Holds no state of its own: the page owns it. */
const ProblemFilters = ({
  query, searchText, tags, onSearchText, onClearSearch, onDifficulty, onToggleTag, onRemoveTag, onTagMode, onSort, onClearAll,
}: ProblemFiltersProps) => (
  <div className="mb-4 space-y-4">
    <div className="grid gap-4 md:grid-cols-[1fr_auto] md:items-end">
      <div>
        <span className="overline mb-1 block">Search</span>
        <SearchBox value={searchText} onChange={onSearchText} onClear={onClearSearch} />
      </div>
      <div>
        <span className="overline mb-1 block">Sort by</span>
        <Select value={query.sort} onChange={(e) => onSort(e.target.value)} className="md:w-48" aria-label="Sort by">
          {sortOptions(Boolean(query.search)).map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </Select>
      </div>
    </div>

    <div>
      <span className="overline mb-1 block">Difficulty</span>
      <DifficultyFilter value={query.difficulty} onChange={onDifficulty} />
    </div>

    <div>
      <span className="overline mb-1 block">Topics</span>
      <TagFilter tags={tags} selected={query.tags} mode={query.tagMode} onToggle={onToggleTag} onModeChange={onTagMode} />
    </div>

    <ActiveFilters
      search={query.search}
      difficulty={query.difficulty === DEFAULT_DIFFICULTY ? null : query.difficulty}
      tags={query.tags}
      onClearSearch={onClearSearch}
      onClearDifficulty={() => onDifficulty(DEFAULT_DIFFICULTY)}
      onRemoveTag={onRemoveTag}
      onClearAll={onClearAll}
    />
  </div>
);

export default ProblemFilters;
