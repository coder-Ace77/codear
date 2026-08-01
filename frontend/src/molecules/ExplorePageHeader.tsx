import { Barcode } from "@/molecules/LandingDecor";

const serif = "font-['Cormorant_Garamond']";
const mono = "font-['Space_Mono']";

const ExplorePageHeader = ({ grandTotalProblems }) => {
  return (
    <div className="mb-10 flex flex-wrap items-end justify-between gap-6 border-b border-border pb-8">
      <div>
        <p className={`${mono} text-[11px] tracking-[0.35em] text-brand-orange mb-4`}>
          THE&nbsp;ARENA
        </p>
        <h1 className={`${serif} uppercase leading-[0.9] text-6xl md:text-8xl text-foreground`}>
          Prob<span className="text-brand-orange">l</span>ems
        </h1>
      </div>

      <div className="flex items-end gap-8">
        <Barcode className="mb-2 hidden sm:flex" color="hsl(var(--muted-foreground))" />

        {/* animated circular counter */}
        <div className="relative w-32 h-32 flex items-center justify-center shrink-0">
          <svg viewBox="0 0 120 120" className="absolute inset-0 w-full h-full animate-spin-slow">
            <circle
              cx="60"
              cy="60"
              r="54"
              fill="none"
              stroke="hsl(var(--brand-orange))"
              strokeWidth="1"
              strokeDasharray="3 7"
              opacity="0.8"
            />
          </svg>
          <svg viewBox="0 0 120 120" className="absolute inset-0 w-full h-full animate-spin-reverse-slow">
            <circle cx="60" cy="60" r="44" fill="none" stroke="hsl(var(--brand-olive))" strokeWidth="1" opacity="0.5" />
            <circle cx="60" cy="6" r="3" fill="hsl(var(--brand-yellow))" />
          </svg>
          <div className="relative text-center">
            <p className={`${serif} text-5xl leading-none text-brand-orange`}>{grandTotalProblems}</p>
            <p className={`${mono} text-[9px] tracking-[0.25em] text-muted-foreground mt-1`}>CHALLENGES</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ExplorePageHeader;
