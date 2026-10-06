import { useEffect, useState, useRef } from "react";
import { Editor } from "@monaco-editor/react";
import { codingService } from "@/service/codingService";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { useResolvedTheme } from "@/hooks/useResolvedTheme";
import { defineProofThemes } from "@/lib/monacoTheme";
import { useSubmissionProgress } from "@/hooks/useSubmissionProgress";
import SubmissionProgress from "@/molecules/SubmissionProgress";


const EditorPanel = ({ code, setCode, problemId, setAcitveTab, setSubmissionId, onSubmissionFinished }) => {
  const [language, setLanguage] = useState(() => localStorage.getItem("preferred-language") || "python");
  const [fontSize, setFontSize] = useState(() => Number(localStorage.getItem("preferred-font-size")) || 14);
  const [output, setOutput] = useState("");
  const [testInput, setTestInput] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();
  const theme = useResolvedTheme();
  const watch = useSubmissionProgress();

  const [isRunningTest, setIsRunningTest] = useState(false);

  const intervalRef = useRef(null);

  useEffect(() => {
    if (problemId && typeof code === 'string') {
      localStorage.setItem(`p${problemId} ${language}`, code);
    }
  }, [code, problemId, language]);

  useEffect(() => {
    if (problemId) {
      const savedCode = localStorage.getItem(`p${problemId} ${language}`);
      if (savedCode) {
        setCode(savedCode);
      } else {
        setCode("");
      }
    }
  }, [problemId]);

  const handleLanguageChange = (newLanguage: string) => {
    if (problemId) {
      localStorage.setItem(`p${problemId} ${language}`, code);
    }

    setLanguage(newLanguage);
    localStorage.setItem("preferred-language", newLanguage);

    if (problemId) {
      const savedCode = localStorage.getItem(`p${problemId} ${newLanguage}`);
      setCode(savedCode || "");
    }
  };

  const handleFontSizeChange = (newSize: number) => {
    setFontSize(newSize);
    localStorage.setItem("preferred-font-size", String(newSize));
  };


  const editorLanguage = {
    python: "python",
    cpp: "cpp",
  }[language];

  // Seconds until the next submission is allowed, after the server answers 429.
  const [cooldown, setCooldown] = useState(0);
  useEffect(() => {
    if (cooldown <= 0) return;
    const id = window.setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => window.clearTimeout(id);
  }, [cooldown]);

  const handleSubmit = async () => {
    try {
      setIsSubmitting(true);
      const { submissionId } = await codingService.submitCode(
        problemId,
        code,
        language
      );
      setSubmissionId(submissionId);
      setIsSubmitting(false);
      // follow it to the verdict here, under the editor; the Submissions tab reloads when it is done
      watch.start(submissionId, () => onSubmissionFinished?.());
    } catch (err: any) {
      console.error(err);
      if (err.response?.status === 429) {
        const header = Number(err.response.headers?.["retry-after"]);
        const fromText = Number(/in (\d+)s/.exec(err.response.data?.detail ?? "")?.[1]);
        const wait = header || fromText || 10;
        setCooldown(wait);
        toast.error(`You can submit again in ${wait} seconds.`);
      } else {
        toast.error("Error submitting code. Please try again");
      }
      setIsSubmitting(false);
    }
  };

  const handleTestCase = async () => {
    try {
      const toastId = toast.loading("Testing code");
      setIsRunningTest(true);

      const { submissionId } = await codingService.runCode(problemId, code, language, testInput);

      const fetchStatus = async () => {
        try {
          const data = await codingService.getRunStatus(submissionId);
          if (data.status === "COMPLETED") {
            toast.success("Test run completed", { id: toastId });
            clearInterval(intervalRef.current);
            setOutput(data.output);
            setIsRunningTest(false);
          }
        } catch (error) {
          clearInterval(intervalRef.current);
          toast.error("Error Running tests", { id: toastId });
        }
      };
      fetchStatus();
      intervalRef.current = setInterval(fetchStatus, 3000);
      setIsRunningTest(false);

    } catch (err) {
      console.error(err);
      toast.error("Error submitting code. Please try again");
      setIsSubmitting(false);
    }
  }

  const fieldClass =
    "h-7 rounded-sm border border-input bg-transparent px-2 font-mono text-[13px] text-foreground";

  return (
    <div className="flex h-full flex-col gap-4 p-6">
      <section className="flex min-h-[320px] flex-1 flex-col overflow-hidden rounded-md border border-border bg-secondary">
        <div className="flex h-10 items-center justify-between gap-3 border-b border-border bg-card px-3">
          <div className="flex items-center gap-2">
            <select
              value={language}
              onChange={(e) => handleLanguageChange(e.target.value)}
              aria-label="Language"
              className={fieldClass}
            >
              <option value="python">Python 3</option>
              <option value="cpp">C++</option>
            </select>

            <select
              value={fontSize}
              onChange={(e) => handleFontSizeChange(Number(e.target.value))}
              aria-label="Font size"
              className={fieldClass}
            >
              <option value="12">12</option>
              <option value="14">14</option>
              <option value="16">16</option>
              <option value="18">18</option>
              <option value="20">20</option>
            </select>
          </div>
        </div>

        <div className="relative min-h-0 flex-1">
          <Editor
            height="100%"
            width="100%"
            language={editorLanguage}
            value={code}
            onChange={(value) => setCode(value || "")}
            beforeMount={defineProofThemes}
            theme={theme === "dark" ? "proof-dark" : "proof-light"}
            loading="Loading editor…"
            options={{
              automaticLayout: true,
              selectOnLineNumbers: true,
              fontSize: fontSize,
              fontFamily: '"IBM Plex Mono", ui-monospace, Menlo, monospace',
              lineHeight: 22,
              minimap: { enabled: false },
              scrollBeyondLastLine: false,
              padding: { top: 12, bottom: 12 },
              renderLineHighlight: "line",
            }}
          />
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2">
        <label className="block">
          <span className="overline mb-1 block">Input</span>
          <textarea
            onChange={(e) => setTestInput(e.target.value)}
            value={testInput}
            className="h-28 w-full resize-none rounded-md border border-input bg-card p-2 font-mono text-[13px] leading-5 text-foreground"
          />
        </label>

        <label className="block">
          <span className="overline mb-1 block">Output</span>
          <textarea
            value={output}
            readOnly
            className="h-28 w-full resize-none rounded-md border border-border bg-secondary p-2 font-mono text-[13px] leading-5 text-foreground"
          />
        </label>
      </section>

      {watch.progress && (
        <SubmissionProgress
          progress={watch.progress}
          phase={watch.phase}
          connectionLost={watch.connectionLost}
          slow={watch.slow}
          error={watch.error}
          onDismiss={watch.reset}
          onViewSubmissions={() => setAcitveTab("submissions")}
        />
      )}

      <div className="flex justify-end gap-3">
        <button
          className="inline-flex h-9 items-center rounded-sm border border-input px-4 text-sm font-semibold transition-colors hover:border-foreground hover:bg-highlight-wash disabled:cursor-not-allowed disabled:border-border disabled:text-muted-foreground"
          onClick={handleTestCase}
          disabled={isRunningTest}
        >
          Run tests
        </button>
        <button
          className="inline-flex h-9 items-center rounded-sm border border-highlight bg-highlight px-4 text-sm font-semibold text-highlight-foreground transition-colors hover:border-primary hover:bg-primary hover:text-primary-foreground disabled:cursor-not-allowed disabled:border-border disabled:bg-secondary disabled:text-muted-foreground"
          onClick={handleSubmit}
          disabled={isSubmitting || watch.phase === "watching" || cooldown > 0}
        >
          {isSubmitting ? "Submitting…" : watch.phase === "watching" ? "Judging…" : cooldown > 0 ? `Wait ${cooldown}s` : "Submit"}
        </button>
      </div>
    </div>
  );
};

export default EditorPanel;
