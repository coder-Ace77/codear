import { X } from "lucide-react";

interface ActiveFiltersProps {
  search: string;
  difficulty: string | null;
  tags: string[];
  onClearSearch: () => void;
  onClearDifficulty: () => void;
  onRemoveTag: (tag: string) => void;
  onClearAll: () => void;
}

const Removable = ({ label, onRemove }: { label: string; onRemove: () => void }) => (
  <span className="inline-flex h-6 items-center gap-1 rounded-full border border-foreground pl-2 pr-1 text-xs">
    {label}
    <button
      type="button"
      onClick={onRemove}
      aria-label={`Remove filter ${label}`}
      className="flex h-4 w-4 items-center justify-center rounded-full hover:bg-highlight-wash"
    >
      <X className="h-3 w-3" aria-hidden="true" />
    </button>
  </span>
);

/** What is narrowing the list right now, each removable, so nobody wonders why a problem is missing. */
const ActiveFilters = ({ search, difficulty, tags, onClearSearch, onClearDifficulty, onRemoveTag, onClearAll }: ActiveFiltersProps) => {
  const count = (search ? 1 : 0) + (difficulty ? 1 : 0) + tags.length;
  if (count === 0) return null;

  return (
    <div className="flex flex-wrap items-center gap-2" aria-label="Active filters">
      <span className="overline">Filtering by</span>
      {search && <Removable label={`"${search}"`} onRemove={onClearSearch} />}
      {difficulty && <Removable label={difficulty} onRemove={onClearDifficulty} />}
      {tags.map((tag) => (
        <Removable key={tag} label={tag} onRemove={() => onRemoveTag(tag)} />
      ))}
      {count > 1 && (
        <button
          type="button"
          onClick={onClearAll}
          className="text-xs font-medium text-muted-foreground underline underline-offset-[3px] hover:text-foreground"
        >
          Clear all
        </button>
      )}
    </div>
  );
};

export default ActiveFilters;
