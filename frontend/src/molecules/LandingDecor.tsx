import { useMemo } from "react";
import { cn } from "@/lib/utils";
import { useInView } from "@/hooks/useInView";

// Deterministic pseudo-random ridge used for the "sound-wave / halftone"
// decoration seen throughout the reference design.
const seededHeights = (n: number, seed: number, rise: number) => {
  let s = seed;
  const rand = () => {
    s = (s * 9301 + 49297) % 233280;
    return s / 233280;
  };
  return Array.from({ length: n }, (_, i) => {
    const t = i / n;
    const base = Math.pow(t, rise); // ridge rising from left to right
    const noise = rand();
    return Math.max(0.03, base * (0.55 + noise * 0.45) + noise * 0.12);
  });
};

interface WaveformProps {
  bars?: number;
  seed?: number;
  rise?: number;
  color?: string;
  className?: string;
  /** flip the ridge horizontally so it rises to the left */
  flip?: boolean;
  /** animate bars growing from the baseline when scrolled into view */
  animate?: boolean;
  /** total stagger window in ms across all bars */
  stagger?: number;
  /** gentle idle "breathing" after the entrance */
  breathe?: boolean;
}

export const Waveform = ({
  bars = 120,
  seed = 7,
  rise = 1.8,
  color = "#c8b48d",
  className,
  flip = false,
  animate = true,
  stagger = 900,
  breathe = false,
}: WaveformProps) => {
  const heights = useMemo(() => {
    const h = seededHeights(bars, seed, rise);
    return flip ? [...h].reverse() : h;
  }, [bars, seed, rise, flip]);

  const { ref, inView } = useInView<HTMLDivElement>({ threshold: 0.15 });
  const on = animate ? inView : true;

  return (
    <div
      ref={ref}
      aria-hidden
      className={cn("flex items-end gap-[2px] w-full h-full select-none pointer-events-none", className)}
    >
      {heights.map((h, i) => {
        // stagger sweeps from the tall side outward for a "materialising" feel
        const delay = (i / bars) * stagger;
        return (
          <div
            key={i}
            className={cn("flex-1 origin-bottom rounded-t-[1px]", breathe && on && "wf-breathe")}
            style={{
              height: `${h * 100}%`,
              backgroundColor: color,
              opacity: on ? 0.85 : 0,
              transform: on ? "scaleY(1)" : "scaleY(0.02)",
              transition: `transform 700ms cubic-bezier(0.16,1,0.3,1) ${delay}ms, opacity 500ms ease ${delay}ms`,
              animationDelay: `${(i % 12) * 120}ms`,
            }}
          />
        );
      })}
    </div>
  );
};

interface BarcodeProps {
  color?: string;
  className?: string;
}

// The tiny barcode glyph that sits in the corners of the reference pages.
export const Barcode = ({ color = "#b39a6d", className }: BarcodeProps) => {
  const widths = useMemo(() => {
    let s = 42;
    const rand = () => {
      s = (s * 9301 + 49297) % 233280;
      return s / 233280;
    };
    return Array.from({ length: 26 }, () => (rand() > 0.5 ? 2 : 1));
  }, []);

  return (
    <div aria-hidden className={cn("flex items-end gap-[2px] h-3 select-none", className)}>
      {widths.map((w, i) => (
        <div
          key={i}
          style={{ width: `${w}px`, backgroundColor: color, height: i % 3 === 0 ? "100%" : "70%" }}
        />
      ))}
    </div>
  );
};
