import { ElementType, ReactNode, CSSProperties } from "react";
import { useInView } from "@/hooks/useInView";
import { cn } from "@/lib/utils";

type Variant = "up" | "down" | "left" | "right" | "scale" | "blur" | "fade";

const hidden: Record<Variant, CSSProperties> = {
  up: { opacity: 0, transform: "translateY(34px)" },
  down: { opacity: 0, transform: "translateY(-34px)" },
  left: { opacity: 0, transform: "translateX(-46px)" },
  right: { opacity: 0, transform: "translateX(46px)" },
  scale: { opacity: 0, transform: "scale(0.86)" },
  blur: { opacity: 0, transform: "translateY(20px)", filter: "blur(10px)" },
  fade: { opacity: 0 },
};

const shown: CSSProperties = {
  opacity: 1,
  transform: "translate(0,0) scale(1)",
  filter: "blur(0px)",
};

interface RevealProps {
  children: ReactNode;
  variant?: Variant;
  delay?: number;
  duration?: number;
  className?: string;
  as?: ElementType;
  once?: boolean;
  threshold?: number;
  style?: CSSProperties;
}

export const Reveal = ({
  children,
  variant = "up",
  delay = 0,
  duration = 800,
  className,
  as: Tag = "div",
  once = true,
  threshold = 0.25,
  style,
}: RevealProps) => {
  const { ref, inView } = useInView<HTMLDivElement>({ threshold, once });

  return (
    <Tag
      ref={ref}
      className={className}
      style={{
        ...style,
        ...(inView ? shown : hidden[variant]),
        transition: `opacity ${duration}ms cubic-bezier(0.22,1,0.36,1) ${delay}ms, transform ${duration}ms cubic-bezier(0.22,1,0.36,1) ${delay}ms, filter ${duration}ms ease ${delay}ms`,
        willChange: "opacity, transform",
      }}
    >
      {children}
    </Tag>
  );
};

interface SplitWordsProps {
  text: string;
  className?: string;
  /** ms between each word */
  stagger?: number;
  /** delay before the first word ms */
  startDelay?: number;
  style?: CSSProperties;
  once?: boolean;
}

/**
 * Splits text into words, each masked behind an overflow-hidden line and
 * revealed from below with a blur — the classic editorial headline entrance.
 */
export const SplitWords = ({
  text,
  className,
  stagger = 90,
  startDelay = 100,
  style,
  once = true,
}: SplitWordsProps) => {
  const { ref, inView } = useInView<HTMLHeadingElement>({ threshold: 0.2, once });
  const words = text.split(" ");

  return (
    <span ref={ref} className={cn("inline", className)} style={style}>
      {words.map((word, i) => (
        <span key={i} className="inline-block overflow-hidden align-bottom pb-[0.12em] -mb-[0.12em]">
          <span
            className="inline-block"
            style={{
              transform: inView ? "translateY(0) rotate(0deg)" : "translateY(112%) rotate(4deg)",
              opacity: inView ? 1 : 0,
              filter: inView ? "blur(0px)" : "blur(6px)",
              transition: `transform 900ms cubic-bezier(0.16,1,0.3,1) ${
                startDelay + i * stagger
              }ms, opacity 700ms ease ${startDelay + i * stagger}ms, filter 700ms ease ${
                startDelay + i * stagger
              }ms`,
              willChange: "transform, opacity, filter",
            }}
          >
            {word}
          </span>
          {i < words.length - 1 && <span>&nbsp;</span>}
        </span>
      ))}
    </span>
  );
};
