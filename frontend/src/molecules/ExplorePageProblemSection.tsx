import { Link } from "react-router-dom";
import Badge from "@/atoms/Badge";
import Chip from "@/atoms/Chip";
import type { ProblemSummary } from "@/types/problemSearch";

interface Props {
  loading: boolean;
  error: string | null;
  problems: ProblemSummary[];
  /** Lower-case tags in the filter, so a row's tag can show it is already chosen. */
  selectedTags: string[];
  filtered: boolean;
  onTagClick: (tag: string) => void;
  onClearFilters: () => void;
}

const Message = ({ title, detail, tone, action }: { title: string; detail?: string; tone?: "danger"; action?: React.ReactNode }) => (
  <div className="border-b border-border py-16 text-center">
    <p className={`font-serif text-2xl font-medium ${tone === "danger" ? "text-danger" : ""}`}>{title}</p>
    {detail && <p className="mt-1 text-muted-foreground">{detail}</p>}
    {action && <div className="mt-4">{action}</div>}
  </div>
);

const acceptanceText = (acceptance: number | null) => (acceptance === null ? "—" : `${acceptance}%`);

const ExplorePageProblemSection = ({ loading, error, problems, selectedTags, filtered, onTagClick, onClearFilters }: Props) => {
  // the first load has nothing to show yet; later loads keep the old rows (dimmed) so the page does not jump
  if (loading && problems.length === 0) return <Message title="Loading problems…" />;
  if (error) return <Message title="Could not load problems" detail={error} tone="danger" />;
  if (!problems.length) {
    return (
      <Message
        title="No problems match"
        detail={filtered ? "Try fewer filters or different words." : "There are no problems yet."}
        action={
          filtered && (
            <button
              type="button"
              onClick={onClearFilters}
              className="inline-flex h-9 items-center rounded-sm border border-input px-4 text-sm font-semibold transition-colors hover:border-foreground hover:bg-highlight-wash"
            >
              Clear filters
            </button>
          )
        }
      />
    );
  }

  return (
    <table className={`w-full border-collapse transition-opacity ${loading ? "opacity-50" : ""}`} aria-busy={loading}>
      <thead>
        <tr className="border-b border-foreground">
          <th className="overline w-px px-3 py-2 text-left">#</th>
          <th className="overline px-3 py-2 text-left">Problem</th>
          <th className="overline hidden px-3 py-2 text-left md:table-cell">Topics</th>
          <th className="overline px-3 py-2 text-left">Difficulty</th>
          <th className="overline hidden px-3 py-2 text-right sm:table-cell">Acceptance</th>
        </tr>
      </thead>
      <tbody>
        {problems.map((problem) => (
          <tr key={problem.id} className="border-b border-border hover:bg-highlight-wash">
            <td className="whitespace-nowrap px-3 py-3 font-mono text-[13px] text-muted-foreground">
              {String(problem.id).padStart(3, "0")}
            </td>
            <td className="px-3 py-3">
              <Link to={`/coding/${problem.id}`} className="font-medium text-foreground underline-offset-[3px] hover:underline">
                {problem.title}
              </Link>
            </td>
            <td className="hidden px-3 py-3 md:table-cell">
              <div className="flex flex-wrap gap-1.5">
                {problem.tags.slice(0, 3).map((tag) => (
                  <Chip key={tag} active={selectedTags.includes(tag.toLowerCase())} onClick={() => onTagClick(tag)} title={`Filter by ${tag}`}>
                    {tag}
                  </Chip>
                ))}
                {problem.tags.length > 3 && (
                  <span className="font-mono text-[13px] text-muted-foreground">+{problem.tags.length - 3}</span>
                )}
              </div>
            </td>
            <td className="px-3 py-3">
              <Badge variant={problem.difficulty.toLowerCase() as "easy" | "medium" | "hard"}>{problem.difficulty}</Badge>
            </td>
            <td
              className="hidden whitespace-nowrap px-3 py-3 text-right font-mono text-[13px] sm:table-cell"
              title={problem.submissions ? `${problem.solvedBy} solved of ${problem.submissions} submissions` : "No submissions yet"}
            >
              {acceptanceText(problem.acceptance)}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};

export default ExplorePageProblemSection;
