import { useState } from "react";
import Chip from "@/atoms/Chip";
import { TAG_MODES, VISIBLE_TAGS } from "@/constants/problemFilters";
import type { TagCount } from "@/types/problemSearch";

interface TagFilterProps {
  tags: TagCount[];
  /** Lower-case tags currently chosen. */
  selected: string[];
  mode: string;
  onToggle: (tag: string) => void;
  onModeChange: (mode: string) => void;
}

const TagFilter = ({ tags, selected, mode, onToggle, onModeChange }: TagFilterProps) => {
  const [showAll, setShowAll] = useState(false);
  if (tags.length === 0) return null;

  // the most used tags, plus any chosen one that would otherwise be hidden
  const visible = showAll
    ? tags
    : tags.filter((t, i) => i < VISIBLE_TAGS || selected.includes(t.tag.toLowerCase()));
  const hidden = tags.length - visible.length;

  return (
    <div className="space-y-2">
      <div role="group" aria-label="Topics" className="flex flex-wrap items-center gap-2">
        {visible.map((t) => (
          <Chip key={t.tag} active={selected.includes(t.tag.toLowerCase())} count={t.count} onClick={() => onToggle(t.tag)}>
            {t.tag}
          </Chip>
        ))}
        {(hidden > 0 || showAll) && tags.length > VISIBLE_TAGS && (
          <button
            type="button"
            onClick={() => setShowAll((v) => !v)}
            className="text-xs font-medium text-muted-foreground underline underline-offset-[3px] hover:text-foreground"
          >
            {showAll ? "Show fewer topics" : `Show all ${tags.length} topics`}
          </button>
        )}
      </div>

      {selected.length > 1 && (
        <div role="group" aria-label="How to combine topics" className="flex items-center gap-2 text-xs text-muted-foreground">
          <span>Problems with</span>
          {TAG_MODES.map((option) => (
            <Chip key={option.value} active={mode === option.value} onClick={() => onModeChange(option.value)}>
              {option.label}
            </Chip>
          ))}
        </div>
      )}
    </div>
  );
};

export default TagFilter;
