import { ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

interface ChipProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  active?: boolean;
  /** A small figure after the label, such as how many problems carry a tag. */
  count?: number;
}

/** A pill that toggles a filter on or off. Selection is shown by the fill and by aria-pressed, not by colour alone. */
const Chip = ({ active = false, count, className, children, ...props }: ChipProps) => (
  <button
    type="button"
    aria-pressed={active}
    className={cn(
      "inline-flex h-6 items-center gap-1.5 whitespace-nowrap rounded-full border px-2 text-xs transition-colors",
      active
        ? "border-foreground bg-foreground text-background"
        : "border-border text-muted-foreground hover:bg-highlight-wash hover:text-foreground",
      className
    )}
    {...props}
  >
    {children}
    {count !== undefined && <span className={cn("font-mono text-[11px]", active ? "opacity-80" : "opacity-70")}>{count}</span>}
  </button>
);

export default Chip;
