import Badge from "@/atoms/Badge";
import { Link } from "react-router-dom";
import apiClient from "@/lib/apiClient";
import { useEffect, useState } from "react";
import { User } from "@/types/User";
import { TagsAndCount } from "@/types/TagsAndCount";
import { Problem, ProblemSummary } from "@/constants/mockData";

const Profile = () => {
  const [user, setUser] = useState<User | null>(null);
  const [totalProblem, setTotalProblems] = useState<number>(0);
  const [recentProblems, setRecentProblems] = useState<ProblemSummary[]>([]);

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const response = await apiClient.get("/user/user");
        const data: User = response.data;
        setUser(data);
      } catch (e) {
        console.error("Unable to load user", e);
      }
    };

    fetchUser();
  }, []);

  useEffect(() => {
    const fetchTotalProblems = async () => {
      try {
        const response = await apiClient.get("/problem/problemCntAndTags");
        const data: TagsAndCount = await response.data;
        setTotalProblems(data.count || 0);
      } catch (e) {
        console.error("Unable to load total problems", e);
      }
    };

    fetchTotalProblems();
  }, []);

  useEffect(() => {
    const fetchRecentProblems = async () => {
      try {
        const response = await apiClient.get("/problem/recent");
        setRecentProblems(response.data);
      } catch (e) {
        console.error("Unable to load recent problems", e);
      }
    };

    fetchRecentProblems();
  }, []);

  if (!user) {
    return (
      <div className="flex min-h-[calc(100vh-3.5rem)] items-center justify-center">
        <p className="text-muted-foreground">Loading profile…</p>
      </div>
    );
  }

  const solvedPercentage =
    totalProblem > 0 ? ((user.problemSolvedTotal ?? 0) / totalProblem) * 100 : 0;

  const counts = [
    { label: "Easy", value: user.problemSolvedEasy, tone: "text-success" },
    { label: "Medium", value: user.problemSolvedMedium, tone: "text-warning" },
    { label: "Hard", value: user.problemSolvedHard, tone: "text-danger" },
  ];

  return (
    <main className="mx-auto max-w-[1600px] px-6 pb-16 pt-12">
      <div className="mb-8 flex flex-wrap items-end justify-between gap-6">
        <div>
          <h1 className="font-serif text-[32px] font-medium leading-[38px] tracking-[-0.01em]">{user.name}</h1>
          <p className="mt-1 font-mono text-[13px] text-muted-foreground">
            @{user.username} · {user.email}
          </p>
        </div>
        {user.dailyStreak ? (
          <span className="mark-fill px-2 py-0.5 font-mono text-xs font-medium">{user.dailyStreak}-day streak</span>
        ) : null}
      </div>

      <section className="mb-10">
        <h2 className="overline mb-2 border-b border-foreground pb-2">Problems solved</h2>
        <p className="mb-2 mt-4">
          <span className="font-mono text-3xl font-medium">{user.problemSolvedTotal ?? 0}</span>
          <span className="font-mono text-[13px] text-muted-foreground"> of {totalProblem}</span>
        </p>
        <div
          className="mb-6 h-1.5 w-full bg-secondary"
          role="progressbar"
          aria-valuenow={Math.round(solvedPercentage)}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label="Share of problems solved"
        >
          <div className="h-full bg-foreground" style={{ width: `${solvedPercentage}%` }} />
        </div>

        <dl className="grid grid-cols-3 gap-4">
          {counts.map((c) => (
            <div key={c.label} className="border-t border-border pt-2">
              <dt className={`text-[13px] font-medium ${c.tone}`}>{c.label}</dt>
              <dd className="font-mono text-2xl font-medium">{c.value ?? 0}</dd>
            </div>
          ))}
        </dl>
      </section>

      <section>
        <h2 className="overline mb-0 border-b border-foreground pb-2">Recently solved</h2>
        {recentProblems.length > 0 ? (
          <table className="w-full border-collapse">
            <tbody>
              {recentProblems.map((problem) => (
                <tr key={problem.id} className="border-b border-border hover:bg-highlight-wash">
                  <td className="w-px whitespace-nowrap px-3 py-3 font-mono text-[13px] text-muted-foreground">
                    {String(problem.id).padStart(3, "0")}
                  </td>
                  <td className="px-3 py-3">
                    <Link to={"/coding/" + problem.id} className="font-medium underline-offset-[3px] hover:underline">
                      {problem.title}
                    </Link>
                  </td>
                  <td className="px-3 py-3 text-right">
                    <Badge variant={problem.difficulty.toLowerCase() as "easy" | "medium" | "hard"}>
                      {problem.difficulty}
                    </Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="py-8 text-center text-muted-foreground">No recent submissions found.</p>
        )}
      </section>
    </main>
  );
};

export default Profile;
