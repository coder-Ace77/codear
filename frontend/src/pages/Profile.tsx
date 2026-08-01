import Badge from "@/atoms/Badge";
import { Award, Flame, TrendingUp, Code2, Link } from "lucide-react"; // Removed 'Calendar' as it wasn't used
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
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-lg animate-pulse">Loading profile...</p>
      </div>
    );
  }

  const solvedPercentage =
    totalProblem > 0 ? (user.problemSolvedTotal / totalProblem) * 100 : 0;

  return (
    <div className="min-h-screen bg-background py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        <div className="bg-card border border-border rounded-xl p-8 mb-6 shadow-sm">
          <div className="flex flex-col md:flex-row items-start md:items-center gap-6">
            <div className="w-24 h-24 rounded-full bg-gradient-primary flex items-center justify-center text-3xl font-bold text-white shadow-glow flex-shrink-0">
              {user.name ? user.name.charAt(0).toUpperCase() : "A"}
            </div>

            <div className="flex-1">
              <h1 className="text-3xl font-bold mb-1">{user.name}</h1>
              <p className="text-muted-foreground mb-3">@{user.username}</p>
              <div className="flex items-center gap-4 text-sm text-muted-foreground">
                <div className="flex items-center gap-1.5">
                  <Award className="w-4 h-4" />
                  <span>{user.email}</span>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-2 px-4 py-2 rounded-lg bg-gradient-accent md:ml-auto">
              <Flame className="w-5 h-5 text-accent-foreground" />
              <div>
                <div className="text-2xl font-bold text-accent-foreground">
                  {user.dailyStreak}
                </div>
                <div className="text-xs text-accent-foreground/80">
                  Day Streak
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="max-w-7xl mx-auto space-y-6">
          <div className="bg-card border border-border rounded-xl p-6 shadow-sm">
            <h2 className="text-xl font-semibold mb-6 flex items-center gap-2">
              <TrendingUp className="w-5 h-5 text-primary" />
              Problems Solved
            </h2>

            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <span className="text-3xl font-bold">
                  {user.problemSolvedTotal}
                </span>
                <span className="text-muted-foreground">
                  / {totalProblem} Total
                </span>
              </div>
              <div className="w-full h-3 bg-secondary rounded-full overflow-hidden">
                <div
                  className="h-full bg-gradient-primary"
                  style={{ width: `${solvedPercentage}%` }}
                />
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div className="p-4 rounded-lg bg-success/10 border border-success/20">
                <div className="text-2xl font-bold text-success mb-1">
                  {user.problemSolvedEasy}
                </div>
                <div className="text-sm text-muted-foreground">Easy</div>
              </div>
              <div className="p-4 rounded-lg bg-warning/10 border border-warning/20">
                <div className="text-2xl font-bold text-warning mb-1">
                  {user.problemSolvedMedium}
                </div>
                <div className="text-sm text-muted-foreground">Medium</div>
              </div>
              <div className="p-4 rounded-lg bg-error/10 border border-error/20">
                <div className="text-2xl font-bold text-error mb-1">
                  {user.problemSolvedHard}
                </div>
                <div className="text-sm text-muted-foreground">Hard</div>
              </div>
            </div>
          </div>

          <div className="bg-card border border-border rounded-xl p-6 shadow-sm">
            <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
              <Code2 className="w-5 h-5 text-primary" />
              Recently Solved
            </h2>

            <div className="space-y-3">
              {recentProblems.length > 0 ? (
                recentProblems.map((problem) => {
                  const difficultyVariant = problem.difficulty.toLowerCase() as
                    | "easy"
                    | "medium"
                    | "hard";
                  return (
                      <div
                        key={problem.id}
                        className="flex items-center justify-between p-3 rounded-lg bg-secondary hover:bg-secondary/80 transition-colors"
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <div className="text-sm font-mono text-muted-foreground">
                            #{problem.id}
                          </div>
                          <span className="font-medium truncate"><a href={"/coding/"+problem.id}>{problem.title}</a></span>
                        </div>
                        <Badge variant={difficultyVariant} className="flex-shrink-0">
                          {problem.difficulty}
                        </Badge>
                      </div>
                  );
                })
              ) : (
                <p className="text-muted-foreground text-sm text-center py-4">
                  No recent submissions found.
                </p>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Profile;