import Badge from "@/atoms/Badge";
import { Problem } from "@/types/problem";

interface ProblemDescriptionProps {
  problem: Problem;
  /** Not shown while a test is running. */
  hideDifficulty?: boolean;
}

const SectionLabel = ({ children }: { children: React.ReactNode }) => (
  <h2 className="overline mb-2 mt-8">{children}</h2>
);

const ProblemDescription: React.FC<ProblemDescriptionProps> = ({ problem, hideDifficulty = false }) => {
  const difficultyVariant = problem.difficulty.toLowerCase() as "easy" | "medium" | "hard";
  const sampleTestCase = problem.testCases?.find((tc) => tc.isSample) || problem.testCases?.[0];
  const constraintItems = problem.constraints
    ? problem.constraints.split("\n").filter((line) => line.trim() !== "")
    : [];

  return (
    <article className="max-w-[62ch] px-6 py-6 pb-12 font-serif text-lg leading-[30px]">
      <h1 className="mb-2 text-[32px] font-medium leading-[38px] tracking-[-0.01em]">
        {problem.title}
      </h1>

      <div className="mb-4 flex flex-wrap items-center gap-3 font-sans text-[13px] leading-5 text-muted-foreground">
        {!hideDifficulty && <Badge variant={difficultyVariant}>{problem.difficulty}</Badge>}
        {problem.tags.length > 0 && <span>{problem.tags.join(" · ")}</span>}
        <span className="font-mono">#{String(problem.id).padStart(3, "0")}</span>
      </div>

      <p className="whitespace-pre-wrap">{problem.description}</p>

      {sampleTestCase && (
        <>
          <SectionLabel>Example</SectionLabel>
          <pre className="whitespace-pre-wrap rounded-md border border-border bg-secondary px-4 py-3 font-mono text-sm leading-[22px]">
            <span className="text-muted-foreground">in  </span>
            {sampleTestCase.input}
            {"\n"}
            <span className="text-muted-foreground">out </span>
            {sampleTestCase.output}
          </pre>
          {problem.inputDescription && (
            <p className="mt-3 font-sans text-sm leading-6 text-muted-foreground">{problem.inputDescription}</p>
          )}
        </>
      )}

      {constraintItems.length > 0 && (
        <>
          <SectionLabel>Constraints</SectionLabel>
          <ul className="space-y-1">
            {constraintItems.map((constraint, index) => (
              <li key={index} className="font-mono text-sm leading-6">
                {constraint}
              </li>
            ))}
          </ul>
        </>
      )}

      <SectionLabel>Limits</SectionLabel>
      <p className="font-sans text-sm text-muted-foreground">
        <span className="font-mono text-foreground">{problem.timeLimitMs} ms</span> time,{" "}
        <span className="font-mono text-foreground">{problem.memoryLimitMb} MB</span> memory.
      </p>
    </article>
  );
};

export default ProblemDescription;
