import { cn } from "@/lib/utils";

type Shape = {
  kind: "blob" | "ring" | "dot";
  color: string;
  size: number;
  top?: string;
  left?: string;
  right?: string;
  bottom?: string;
  anim: string;
  delay?: string;
  opacity?: number;
};

const O = "hsl(var(--brand-orange))";
const Y = "hsl(var(--brand-yellow))";
const L = "hsl(var(--brand-olive))";
const R = "hsl(var(--brand-rust))";
const G = "hsl(var(--accent))";

const SHAPES: Shape[] = [
  { kind: "blob", color: O, size: 340, top: "-6%", left: "-4%", anim: "animate-drift-a", opacity: 0.18 },
  { kind: "blob", color: L, size: 420, top: "30%", right: "-8%", anim: "animate-drift-b", opacity: 0.14, delay: "-6s" },
  { kind: "blob", color: R, size: 300, bottom: "-8%", left: "18%", anim: "animate-drift-c", opacity: 0.13, delay: "-3s" },
  { kind: "blob", color: Y, size: 240, top: "55%", left: "-6%", anim: "animate-drift-a", opacity: 0.12, delay: "-9s" },
  { kind: "ring", color: G, size: 220, top: "12%", right: "10%", anim: "animate-spin-slow", opacity: 0.35 },
  { kind: "ring", color: O, size: 140, bottom: "16%", right: "22%", anim: "animate-spin-reverse-slow", opacity: 0.4, delay: "-4s" },
  { kind: "ring", color: Y, size: 300, bottom: "-10%", right: "-6%", anim: "animate-spin-slow", opacity: 0.25 },
  { kind: "dot", color: R, size: 12, top: "22%", left: "42%", anim: "animate-float-y", opacity: 0.7 },
  { kind: "dot", color: L, size: 8, top: "70%", right: "34%", anim: "animate-float-y", opacity: 0.7, delay: "-2s" },
  { kind: "dot", color: Y, size: 10, top: "40%", left: "20%", anim: "animate-float-y", opacity: 0.6, delay: "-4s" },
];

interface Props {
  className?: string;
}

/** Ambient animated background: drifting colored blobs, slow rotating rings,
 *  and floating accent dots. Purely decorative. */
const FloatingShapes = ({ className }: Props) => (
  <div aria-hidden className={cn("absolute inset-0 overflow-hidden pointer-events-none", className)}>
    {SHAPES.map((s, i) => {
      const base: React.CSSProperties = {
        position: "absolute",
        width: s.size,
        height: s.size,
        top: s.top,
        left: s.left,
        right: s.right,
        bottom: s.bottom,
        opacity: s.opacity,
        animationDelay: s.delay,
      };

      if (s.kind === "blob") {
        return (
          <span
            key={i}
            className={s.anim}
            style={{ ...base, background: s.color, borderRadius: "50%", filter: "blur(70px)" }}
          />
        );
      }
      if (s.kind === "ring") {
        return (
          <span
            key={i}
            className={s.anim}
            style={{
              ...base,
              borderRadius: "50%",
              border: `1px solid ${s.color}`,
              // dashed inner feel via a subtle second inset ring
              boxShadow: `inset 0 0 0 8px transparent, inset 0 0 0 9px ${s.color}22`,
            }}
          />
        );
      }
      return (
        <span
          key={i}
          className={s.anim}
          style={{ ...base, background: s.color, borderRadius: "50%", filter: `drop-shadow(0 0 8px ${s.color})` }}
        />
      );
    })}
  </div>
);

export default FloatingShapes;
