import { Loader2, AlertTriangle } from "lucide-react";
import ProblemCard from "./ProblemCard";
import { Reveal } from "@/molecules/Reveal";

const mono = "font-['Space_Mono']";

const ExplorePageProblemSection = ({ loading, problemsummary, error }) => {
  return (
    <div className="grid gap-3 mb-8">
      {loading ? (
        <div className="flex justify-center items-center py-16 gap-3">
          <Loader2 className="w-6 h-6 animate-spin text-accent" />
          <p className={`${mono} text-xs tracking-[0.2em] uppercase text-muted-foreground`}>
            Loading problems
          </p>
        </div>
      ) : error ? (
        <div className="flex flex-col items-center justify-center text-center py-16 bg-card border border-error/40 rounded-md">
          <AlertTriangle className="w-10 h-10 text-error mb-4" />
          <p className={`${mono} text-sm tracking-[0.15em] uppercase text-error`}>
            An error occurred
          </p>
          <p className="text-muted-foreground mt-2 text-sm">{error}</p>
        </div>
      ) : problemsummary.length > 0 ? (
        problemsummary.map((problem, i) => (
          <Reveal key={problem.id} variant="up" delay={Math.min(i, 8) * 60} duration={600}>
            <ProblemCard problem={problem} />
          </Reveal>
        ))
      ) : (
        <div className="text-center py-16 bg-card border border-border rounded-md">
          <p className={`${mono} text-xs tracking-[0.2em] uppercase text-muted-foreground`}>
            No problems found matching your criteria
          </p>
        </div>
      )}
    </div>
  );
};

export default ExplorePageProblemSection;
