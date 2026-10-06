import { Glyph, styleFor } from "@/lib/verdict";
import { cn } from "@/lib/utils";

const Mark = ({ kind }: { kind: Glyph }) => (
  <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="square" aria-hidden="true" className="shrink-0">
    {kind === "check" && <path d="M2 6.4l2.6 2.6L10 3.4" />}
    {kind === "cross" && <path d="M2.5 2.5l7 7M9.5 2.5l-7 7" />}
    {kind === "clock" && (
      <>
        <circle cx="6" cy="6" r="4.6" />
        <path d="M6 3.4V6l1.8 1.1" />
      </>
    )}
    {kind === "ring" && <circle cx="6" cy="6" r="4.6" strokeDasharray="3 2" />}
  </svg>
);

/** A glyph, a word and a wash: never colour alone. `status` is a verdict name or a plain status. */
const Verdict = ({ status, className }: { status: string; className?: string }) => {
  const style = styleFor(status);
  return (
    <span className={cn("inline-flex items-center gap-1.5 whitespace-nowrap rounded-sm px-2 py-0.5 font-mono text-xs font-medium", style.tone, className)}>
      <Mark kind={style.glyph} />
      {style.label}
    </span>
  );
};

export default Verdict;
