import Chip from "@/atoms/Chip";
import { DIFFICULTY_OPTIONS } from "@/constants/problemFilters";

interface DifficultyFilterProps {
  value: string;
  onChange: (value: string) => void;
}

const DifficultyFilter = ({ value, onChange }: DifficultyFilterProps) => (
  <div role="group" aria-label="Difficulty" className="flex flex-wrap gap-2">
    {DIFFICULTY_OPTIONS.map((option) => (
      <Chip key={option.value} active={value === option.value} onClick={() => onChange(option.value)}>
        {option.label}
      </Chip>
    ))}
  </div>
);

export default DifficultyFilter;
