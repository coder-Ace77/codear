import Badge from "@/atoms/Badge";
import { Clock, Database } from "lucide-react";
import { Problem } from "@/types/problem";

interface ProblemDescriptionProps {
  problem: Problem;
}

const serif = "font-['Cormorant_Garamond']";
const mono = "font-['Space_Mono']";

const tagColors = [
  "hsl(var(--brand-orange))",
  "hsl(var(--brand-yellow))",
  "hsl(var(--brand-olive))",
  "hsl(var(--brand-clay))",
  "hsl(var(--brand-rust))",
];

const SectionLabel = ({ children, color }: { children: React.ReactNode; color: string }) => (
  <div className="flex items-center gap-2 mb-3">
    <span className="inline-block w-2 h-2 rounded-full" style={{ backgroundColor: color, boxShadow: `0 0 8px ${color}` }} />
    <h2 className={`${mono} text-[11px] tracking-[0.25em] uppercase`} style={{ color }}>
      {children}
    </h2>
  </div>
);

const ProblemDescription: React.FC<ProblemDescriptionProps> = ({ problem }) => {
  const difficultyVariant = problem.difficulty.toLowerCase() as "easy" | "medium" | "hard";
  const sampleTestCase = problem.testCases?.find((tc) => tc.isSample) || problem.testCases?.[0];
  const constraintItems = problem.constraints
    ? problem.constraints.split("\n").filter((line) => line.trim() !== "")
    : [];

  return (
    <div className="p-6">
      <p className={`${mono} text-[10px] tracking-[0.3em] text-brand-orange mb-2`}>
        PROBLEM&nbsp;#{String(problem.id).padStart(3, "0")}
      </p>
      <h1 className={`${serif} text-3xl md:text-4xl leading-tight text-foreground mb-4`}>
        {problem.title}
      </h1>

      <div className="flex items-center gap-2 mb-8 flex-wrap">
        <Badge variant={difficultyVariant}>{problem.difficulty}</Badge>
        {problem.tags.map((tag, i) => (
          <span
            key={tag}
            className={`${mono} text-[10px] tracking-[0.15em] uppercase border rounded px-2 py-0.5`}
            style={{
              color: tagColors[i % tagColors.length],
              borderColor: tagColors[i % tagColors.length],
            }}
          >
            {tag}
          </span>
        ))}
      </div>

      <div className="space-y-8">
        <div>
          <SectionLabel color="hsl(var(--brand-olive))">Description</SectionLabel>
          <p className="text-foreground text-[15px] md:text-base leading-7 whitespace-pre-wrap">
            {problem.description}
          </p>
        </div>

        {sampleTestCase && (
          <div>
            <SectionLabel color="hsl(var(--brand-yellow))">Example</SectionLabel>
            <div
              className="rounded-lg p-4 font-mono text-sm space-y-3 border-l-2 bg-secondary/60"
              style={{ borderColor: "hsl(var(--brand-orange))" }}
            >
              <div>
                <div className={`${mono} text-[10px] tracking-[0.2em] uppercase text-brand-olive`}>Input</div>
                <pre className="text-foreground whitespace-pre-wrap mt-1">{sampleTestCase.input}</pre>
              </div>
              <div>
                <div className={`${mono} text-[10px] tracking-[0.2em] uppercase text-brand-orange`}>Output</div>
                <pre className="text-foreground whitespace-pre-wrap mt-1">{sampleTestCase.output}</pre>
              </div>

              {problem.inputDescription && (
                <div className="font-sans pt-2">
                  <p className="text-muted-foreground">{problem.inputDescription}</p>
                </div>
              )}
            </div>
          </div>
        )}

        <div>
          <SectionLabel color="hsl(var(--brand-rust))">Constraints</SectionLabel>
          <ul className="space-y-2">
            {constraintItems.map((constraint, index) => (
              <li key={index} className="flex items-start gap-3 font-mono text-sm text-foreground/80">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full shrink-0" style={{ backgroundColor: "hsl(var(--brand-rust))" }} />
                {constraint}
              </li>
            ))}
          </ul>
        </div>

        <div className="pt-4 border-t border-border">
          <div className="flex items-center gap-6 text-sm">
            <div className="flex items-center gap-2 text-muted-foreground">
              <Clock className="w-4 h-4 text-brand-yellow" />
              <span>Time Limit: <span className="text-foreground">{problem.timeLimitMs} ms</span></span>
            </div>
            <div className="flex items-center gap-2 text-muted-foreground">
              <Database className="w-4 h-4 text-brand-olive" />
              <span>Memory: <span className="text-foreground">{problem.memoryLimitMb} MB</span></span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProblemDescription;
