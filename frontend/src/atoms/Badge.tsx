import { HTMLAttributes, forwardRef } from "react";
import { cn } from "@/lib/utils";

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: "easy" | "medium" | "hard" | "default";
}

const Badge = forwardRef<HTMLSpanElement, BadgeProps>(
  ({ className, variant = "default", children, ...props }, ref) => {
    const variants = {
      easy: "bg-brand-olive/20 text-brand-olive border-brand-olive/40",
      medium: "bg-brand-yellow/20 text-brand-yellow border-brand-yellow/40",
      hard: "bg-brand-orange/20 text-brand-orange border-brand-orange/40",
      default: "bg-secondary text-secondary-foreground border-border",
    };

    return (
      <span
        ref={ref}
        className={cn(
          "inline-flex items-center px-2.5 py-0.5 rounded-md text-xs font-medium border transition-all duration-200",
          variants[variant],
          className
        )}
        {...props}
      >
        {children}
      </span>
    );
  }
);

Badge.displayName = "Badge";

export default Badge;
