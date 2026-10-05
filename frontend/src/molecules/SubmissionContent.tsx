import { useEffect, useState } from "react";
import type { Submission } from "@/types/submission";
import apiClient from "@/lib/apiClient";
import toast from "react-hot-toast";
import { format } from "date-fns";
import { ChevronDown } from "lucide-react";
import Verdict from "@/atoms/Verdict";
import CodeBlock, { CopyButton } from "./CodeBlock";

const LogBlock = ({ label, text }: { label: string; text: string }) => (
  <details className="group border-t border-border">
    <summary className="flex cursor-pointer list-none items-center justify-between px-4 py-2 text-[13px] font-medium text-danger hover:bg-highlight-wash">
      <span>{label}</span>
      <ChevronDown className="h-4 w-4 text-muted-foreground transition-transform group-open:rotate-180" aria-hidden="true" />
    </summary>
    <div className="relative bg-secondary">
      <div className="absolute right-2 top-2 z-10">
        <CopyButton text={text} />
      </div>
      <pre className="overflow-x-auto whitespace-pre-wrap p-4 pr-20 font-mono text-xs leading-5">{text}</pre>
    </div>
  </details>
);

const Stat = ({ label, value }: { label: string; value: React.ReactNode }) => (
  <div>
    <span className="overline block">{label}</span>
    <p className="font-mono text-[13px] font-medium">{value}</p>
  </div>
);

export const SubmissionsContent = ({ problemId }: { problemId: number | string }) => {
  const [submissions, setSubmissions] = useState<Submission[] | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [refresh, setRefreshing] = useState(false);

  useEffect(() => {
    if (!problemId) {
      setSubmissions([]);
      return;
    }

    const fetchSubmissions = async () => {
      setIsLoading(true);
      setSubmissions(null);
      try {
        const response = await apiClient.get(`/problem/submissions/subuser/${problemId}`);
        const data: Submission[] = response.data;
        data.sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime());
        setSubmissions(data);
      } catch (e) {
        console.error("Error fetching submissions:", e);
        toast.error("Failed to load submissions.");
        setSubmissions([]);
      } finally {
        setIsLoading(false);
      }
    };
    fetchSubmissions();
  }, [refresh, problemId]);

  const handleRefresh = () => setRefreshing((r) => !r);

  const renderContent = () => {
    if (isLoading) {
      return <p className="py-12 text-center text-muted-foreground">Loading submissions…</p>;
    }

    if (!submissions || submissions.length === 0) {
      return <p className="py-12 text-center text-muted-foreground">You have not submitted a solution yet.</p>;
    }

    return (
      <div className="space-y-4">
        {submissions.map((sub) => {
          const formattedDate = format(new Date(sub.submittedAt), "MMM d, yyyy 'at' h:mm a");

          return (
            <div key={sub.id} className="overflow-hidden rounded-md border border-border bg-card">
              <div className="flex items-center justify-between gap-3 px-4 py-2">
                <div className="flex min-w-0 items-center gap-3">
                  <Verdict status={sub.status} />
                  <span className="truncate text-[13px] text-muted-foreground">{formattedDate}</span>
                </div>
                <span className="shrink-0 font-mono text-[13px] text-muted-foreground">{sub.language}</span>
              </div>

              <div className="grid grid-cols-3 gap-4 border-t border-border px-4 py-3">
                <Stat label="Tests" value={`${sub.passedTests}/${sub.totalTests}`} />
                <Stat label="Time" value={`${sub.timeTakenMs} ms`} />
                <Stat label="Memory" value={sub.memoryUsed} />
              </div>

              {sub.errorLog ? (
                <LogBlock label="Execution log" text={sub.errorLog} />
              ) : (
                sub.status === "FAILED" && sub.result && <LogBlock label="Failure details" text={sub.result} />
              )}

              <details className="group border-t border-border">
                <summary className="flex cursor-pointer list-none items-center justify-between px-4 py-2 text-[13px] font-medium hover:bg-highlight-wash">
                  <span>View submitted code</span>
                  <ChevronDown className="h-4 w-4 text-muted-foreground transition-transform group-open:rotate-180" aria-hidden="true" />
                </summary>
                <CodeBlock code={sub.code} language={sub.language} bare />
              </details>
            </div>
          );
        })}
      </div>
    );
  };

  return (
    <div className="max-w-4xl px-6 py-6">
      <div className="mb-4 flex items-baseline justify-between">
        <h2 className="font-serif text-2xl font-medium">Submissions</h2>
        <button
          onClick={handleRefresh}
          className="inline-flex h-7 items-center rounded-sm border border-input px-3 text-[13px] font-semibold transition-colors hover:border-foreground hover:bg-highlight-wash"
        >
          Refresh
        </button>
      </div>
      {renderContent()}
    </div>
  );
};

export default SubmissionsContent;
