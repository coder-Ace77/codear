import { ButtonHTMLAttributes, forwardRef } from "react";
import { cn } from "@/lib/utils";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "accent" | "ghost" | "outline" | "destructive";
  size?: "sm" | "md" | "lg";
}

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "primary", size = "md", children, ...props }, ref) => {
    const baseStyles =
      "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-sm border font-sans font-semibold transition-colors disabled:cursor-not-allowed disabled:border-border disabled:bg-paper-sunken disabled:text-ink-muted";

    // Primary is ink on paper and turns to the marker on hover; "accent" is the marker itself (Submit).
    const variants = {
      primary: "border-primary bg-primary text-primary-foreground hover:border-highlight hover:bg-highlight hover:text-highlight-foreground",
      accent: "border-highlight bg-highlight text-highlight-foreground hover:border-primary hover:bg-primary hover:text-primary-foreground",
      secondary: "border-border bg-secondary text-secondary-foreground hover:bg-highlight-wash",
      outline: "border-input bg-transparent text-foreground hover:border-foreground hover:bg-highlight-wash",
      ghost: "border-transparent bg-transparent text-foreground hover:bg-highlight-wash",
      destructive: "border-danger bg-danger text-destructive-foreground hover:opacity-90",
    };

    const sizes = {
      sm: "h-7 px-3 text-[13px]",
      md: "h-9 px-4 text-sm",
      lg: "h-11 px-6 text-[15px]",
    };

    return (
      <button
        ref={ref}
        className={cn(baseStyles, variants[variant], sizes[size], className)}
        {...props}
      >
        {children}
      </button>
    );
  }
);

Button.displayName = "Button";

export default Button;
