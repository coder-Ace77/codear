import { useEffect, useState } from "react";
import type { Submission } from "@/types/submission";
import apiClient from "@/lib/apiClient";
import toast from "react-hot-toast";
import { format } from "date-fns";
import {
  CheckCircle,
  Loader2,
  ChevronDown,
  Code,
  LineChart,
  Database,
  FileText,
  RefreshCw,
  Terminal,
} from "lucide-react";
import { getStatusStyles } from "@/constants/subStyles";
import CodeBlock, { CopyButton } from "./CodeBlock";

const mono = "font-['Space_Mono']";
const serif = "font-['Cormorant_Garamond']";

const LogBlock = ({
  label,
  color,
  text,
}: {
  label: string;
  color: string;
  text: string;
}) => (
  <details className="border-t border-border group">
    <summary className={`px-4 py-2.5 flex items-center justify-between cursor-pointer hover:bg-white/5 ${mono} text-[11px] tracking-[0.15em] uppercase list-none`}>
      <div className="flex items-center gap-2" style={{ color }}>
        <Terminal className="w-4 h-4" />
        <span>{label}</span>
      </div>
      <ChevronDown className="w-4 h-4 text-muted-foreground transition-transform group-open:rotate-180" />
    </summary>
    <div className="relative" style={{ backgroundColor: "#282828" }}>
      <div className="absolute top-2 right-2 z-10">
        <CopyButton text={text} />
      </div>
      <pre
        className="p-4 pr-16 font-mono text-xs overflow-x-auto whitespace-pre-wrap"
        style={{ color }}
      >
        {text}
      </pre>
    </div>
  </details>
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
      return (
        <div className="flex justify-center items-center h-48 gap-3">
          <Loader2 className="w-6 h-6 animate-spin text-brand-orange" />
          <p className={`${mono} text-xs tracking-[0.2em] uppercase text-muted-foreground`}>
            Loading submissions
          </p>
        </div>
      );
    }

    if (!submissions || submissions.length === 0) {
      return (
        <p className={`${mono} text-xs tracking-[0.15em] uppercase text-muted-foreground text-center py-12`}>
          No submissions for this problem yet.
        </p>
      );
    }

    return (
      <div className="space-y-4">
        {submissions.map((sub) => {
          const { icon, color, bgColor } = getStatusStyles(sub.status);
          const formattedDate = format(new Date(sub.submittedAt), "MMM d, yyyy 'at' h:mm a");

          return (
            <div key={sub.id} className="bg-card border border-border rounded-lg overflow-hidden transition-all hover:border-brand-orange/40">
              <div className={`px-4 py-2.5 flex items-center justify-between gap-3 ${bgColor}`}>
                <div className="flex items-center gap-2.5 min-w-0">
                  <span className={color}>{icon}</span>
                  <span className={`${mono} text-xs tracking-[0.12em] uppercase ${color}`}>{sub.status}</span>
                  <span className="text-muted-foreground/50">·</span>
                  <span className="text-[11px] text-muted-foreground truncate">{formattedDate}</span>
                </div>

                <div className={`${mono} flex items-center gap-1.5 text-[10px] tracking-[0.15em] uppercase text-muted-foreground shrink-0`}>
                  <FileText className="w-3.5 h-3.5 text-brand-orange" />
                  <span>{sub.language}</span>
                </div>
              </div>

              <div className="p-4 grid grid-cols-2 sm:grid-cols-3 gap-4 text-sm border-t border-border">
                <div className="flex items-center gap-2">
                  <CheckCircle className="w-4 h-4 text-brand-olive" />
                  <div>
                    <span className={`${mono} text-[9px] tracking-[0.15em] uppercase text-muted-foreground`}>Tests</span>
                    <p className="font-semibold text-foreground">{sub.passedTests} / {sub.totalTests}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <LineChart className="w-4 h-4 text-brand-clay" />
                  <div>
                    <span className={`${mono} text-[9px] tracking-[0.15em] uppercase text-muted-foreground`}>Time</span>
                    <p className="font-semibold text-foreground">{sub.timeTakenMs} ms</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Database className="w-4 h-4 text-brand-yellow" />
                  <div>
                    <span className={`${mono} text-[9px] tracking-[0.15em] uppercase text-muted-foreground`}>Memory</span>
                    <p className="font-semibold text-foreground">{sub.memoryUsed}</p>
                  </div>
                </div>
              </div>

              {sub.errorLog ? (
                <LogBlock label="Execution Logs" color="hsl(var(--brand-rust))" text={sub.errorLog} />
              ) : (
                sub.status === "FAILED" && sub.result && (
                  <LogBlock label="Failure Details" color="hsl(var(--brand-orange))" text={sub.result} />
                )
              )}

              <details className="border-t border-border group">
                <summary className={`px-4 py-2.5 flex items-center justify-between cursor-pointer hover:bg-white/5 ${mono} text-[11px] tracking-[0.15em] uppercase text-muted-foreground list-none`}>
                  <div className="flex items-center gap-2">
                    <Code className="w-4 h-4" />
                    <span>View Submitted Code</span>
                  </div>
                  <ChevronDown className="w-4 h-4 transition-transform group-open:rotate-180" />
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
    <div className="p-4 md:p-6 max-w-4xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <div>
          <p className={`${mono} text-[10px] tracking-[0.3em] uppercase text-brand-orange mb-1`}>Your History</p>
          <h2 className={`${serif} text-3xl text-foreground`}>Submissions</h2>
        </div>

        <button
          onClick={handleRefresh}
          className={`${mono} flex items-center gap-2 text-[11px] tracking-[0.15em] uppercase px-4 py-2.5 rounded-full border border-border text-foreground hover:border-brand-orange hover:text-brand-orange transition-colors`}
        >
          <RefreshCw className="w-4 h-4" />
          Refresh
        </button>
      </div>
      {renderContent()}
    </div>
  );
};

export default SubmissionsContent;
