import { Search, X } from "lucide-react";
import Input from "@/atoms/Input";
import { SEARCH_MAX_LENGTH } from "@/constants/problemFilters";

interface SearchBoxProps {
  value: string;
  onChange: (value: string) => void;
  onClear: () => void;
}

const SearchBox = ({ value, onChange, onClear }: SearchBoxProps) => (
  <div className="relative">
    <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
    <Input
      type="search"
      value={value}
      maxLength={SEARCH_MAX_LENGTH}
      onChange={(e) => onChange(e.target.value)}
      placeholder="Search by title, statement or tag"
      aria-label="Search problems"
      className="px-9 [&::-webkit-search-cancel-button]:hidden"
    />
    {value && (
      <button
        type="button"
        onClick={onClear}
        aria-label="Clear search"
        className="absolute right-2 top-1/2 flex h-5 w-5 -translate-y-1/2 items-center justify-center rounded-sm text-muted-foreground hover:text-foreground"
      >
        <X className="h-3.5 w-3.5" aria-hidden="true" />
      </button>
    )}
  </div>
);

export default SearchBox;
