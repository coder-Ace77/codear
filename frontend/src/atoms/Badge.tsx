import { HTMLAttributes, forwardRef } from "react";
import { cn } from "@/lib/utils";

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: "easy" | "medium" | "hard" | "default";
}

const PIPS = { easy: 1, medium: 2, hard: 3 } as const;

const tone = {
  easy: "text-success",
  medium: "text-warning",
  hard: "text-danger",
};

// Difficulty is a word plus one to three pips, so it never relies on colour alone.
const Badge = forwardRef<HTMLSpanElement, BadgeProps>(
  ({ className, variant = "default", children, ...props }, ref) => {
    if (variant === "default") {
      return (
        <span
          ref={ref}
          className={cn(
            "inline-flex h-6 items-center rounded-sm border border-border bg-secondary px-2 font-sans text-xs text-secondary-foreground",
            className
          )}
          {...props}
        >
          {children}
        </span>
      );
    }

    const filled = PIPS[variant];
    return (
      <span
        ref={ref}
        className={cn("inline-flex items-center gap-1.5 whitespace-nowrap font-sans text-[13px] font-medium", tone[variant], className)}
        {...props}
      >
        <span aria-hidden="true" className="inline-flex gap-0.5">
          {[0, 1, 2].map((i) => (
            <i
              key={i}
              className={cn("block h-1.5 w-1.5 border border-current", i < filled && "bg-current")}
            />
          ))}
        </span>
        {children}
      </span>
    );
  }
);

Badge.displayName = "Badge";

export default Badge;
