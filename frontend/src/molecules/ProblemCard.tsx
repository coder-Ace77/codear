import { Link } from "react-router-dom";
import Badge from "@/atoms/Badge";
import { ProblemSummary } from "@/constants/mockData";
import { ArrowUpRight } from "lucide-react";

interface ProblemCardProps {
  problem: ProblemSummary;
}

const serif = "font-['Cormorant_Garamond']";
const mono = "font-['Space_Mono']";

const tagColors = [
  "hsl(var(--brand-orange))",
  "hsl(var(--brand-yellow))",
  "hsl(var(--brand-olive))",
  "hsl(var(--brand-clay))",
];

const ProblemCard = ({ problem }: ProblemCardProps) => {
  const difficultyVariant = problem.difficulty.toLowerCase() as "easy" | "medium" | "hard";

  return (
    <Link to={`/coding/${problem.id}`}>
      <div className="group flex items-center gap-4 md:gap-6 px-5 md:px-7 py-5 bg-card border border-border rounded-md hover:border-accent hover:bg-secondary/40 transition-all duration-300 cursor-pointer">
        <span className={`${mono} text-sm text-muted-foreground w-10 md:w-14 shrink-0 tabular-nums`}>
          {String(problem.id).padStart(3, "0")}
        </span>

        <div className="flex-1 min-w-0">
          <h3
            className={`${serif} text-2xl md:text-3xl leading-tight text-foreground group-hover:text-accent transition-colors truncate`}
          >
            {problem.title}
          </h3>
          <div className="flex items-center gap-2 flex-wrap mt-2">
            <Badge variant={difficultyVariant}>{problem.difficulty}</Badge>
            {problem.tags.slice(0, 3).map((tag, i) => (
              <span
                key={tag}
                className={`${mono} text-[10px] tracking-[0.15em] uppercase border rounded px-2 py-0.5`}
                style={{ color: tagColors[i % tagColors.length], borderColor: `${tagColors[i % tagColors.length]}` }}
              >
                {tag}
              </span>
            ))}
            {problem.tags.length > 3 && (
              <span className={`${mono} text-[10px] text-muted-foreground`}>
                +{problem.tags.length - 3}
              </span>
            )}
          </div>
        </div>

        <ArrowUpRight className="w-5 h-5 text-muted-foreground shrink-0 transition-all duration-300 group-hover:text-accent group-hover:translate-x-1 group-hover:-translate-y-1" />
      </div>
    </Link>
  );
};

export default ProblemCard;
