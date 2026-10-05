import { Link } from "react-router-dom";
import Badge from "@/atoms/Badge";

const Message = ({ title, detail, tone }: { title: string; detail?: string; tone?: "danger" }) => (
  <div className="border-b border-border py-16 text-center">
    <p className={`font-serif text-2xl font-medium ${tone === "danger" ? "text-danger" : ""}`}>{title}</p>
    {detail && <p className="mt-1 text-muted-foreground">{detail}</p>}
  </div>
);

const ExplorePageProblemSection = ({ loading, problemsummary, error }) => {
  if (loading) return <Message title="Loading problems…" />;
  if (error) return <Message title="Could not load problems" detail={error} tone="danger" />;
  if (!problemsummary.length)
    return <Message title="No problems match" detail="Try a different search or clear the filters." />;

  return (
    <table className="w-full border-collapse">
      <thead>
        <tr className="border-b border-foreground">
          <th className="overline w-px px-3 py-2 text-left">#</th>
          <th className="overline px-3 py-2 text-left">Problem</th>
          <th className="overline hidden px-3 py-2 text-left md:table-cell">Topics</th>
          <th className="overline px-3 py-2 text-left">Difficulty</th>
        </tr>
      </thead>
      <tbody>
        {problemsummary.map((problem) => (
          <tr key={problem.id} className="border-b border-border hover:bg-highlight-wash">
            <td className="whitespace-nowrap px-3 py-3 font-mono text-[13px] text-muted-foreground">
              {String(problem.id).padStart(3, "0")}
            </td>
            <td className="px-3 py-3">
              <Link
                to={`/coding/${problem.id}`}
                className="font-medium text-foreground underline-offset-[3px] hover:underline"
              >
                {problem.title}
              </Link>
            </td>
            <td className="hidden px-3 py-3 md:table-cell">
              <div className="flex gap-1.5">
                {problem.tags.slice(0, 3).map((tag) => (
                  <span
                    key={tag}
                    className="inline-flex h-6 items-center whitespace-nowrap rounded-full border border-border px-2 text-xs text-muted-foreground"
                  >
                    {tag}
                  </span>
                ))}
                {problem.tags.length > 3 && (
                  <span className="font-mono text-[13px] text-muted-foreground">+{problem.tags.length - 3}</span>
                )}
              </div>
            </td>
            <td className="px-3 py-3">
              <Badge variant={problem.difficulty.toLowerCase() as "easy" | "medium" | "hard"}>
                {problem.difficulty}
              </Badge>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};

export default ExplorePageProblemSection;
