import { useEffect, useState, useRef } from "react";
import { Play, CheckCircle } from "lucide-react";
import { Editor } from "@monaco-editor/react";
import { getButtonClasses } from "@/constants/ButtonVariants";
import { codingService } from "@/service/codingService";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";


const EditorPanel = ({ code, setCode, problemId, setAcitveTab, setSubmissionId }) => {
  const [language, setLanguage] = useState(() => localStorage.getItem("preferred-language") || "python");
  const [fontSize, setFontSize] = useState(() => Number(localStorage.getItem("preferred-font-size")) || 14);
  const [output, setOutput] = useState("");
  const [testInput, setTestInput] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

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

  const handleSubmit = async () => {
    try {
      setIsSubmitting(true);
      const { submissionId } = await codingService.submitCode(
        problemId,
        code,
        language
      );
      setSubmissionId(submissionId);
      setAcitveTab('submissions');
      setIsSubmitting(false);
    } catch (err) {

      console.error(err);
      toast.error("Error submitting code. Please try again");
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

  return (
    <div className="h-full flex flex-col bg-card border border-border rounded-xl overflow-hidden">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between px-4 py-3 border-b border-border bg-secondary gap-4 sm:gap-0">
        <div className="flex items-center gap-3 w-full sm:w-auto">
          <select
            value={language}
            onChange={(e) => handleLanguageChange(e.target.value)}
            className="flex-1 sm:flex-none sm:w-40 h-9 px-3 rounded-md border border-border bg-transparent text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="python">Python</option>
            <option value="cpp">C++</option>
          </select>

          <select
            value={fontSize}
            onChange={(e) => handleFontSizeChange(Number(e.target.value))}
            className="w-20 h-9 px-3 rounded-md border border-border bg-transparent text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="12">12</option>
            <option value="14">14</option>
            <option value="16">16</option>
            <option value="18">18</option>
            <option value="20">20</option>
          </select>
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
          <button className={getButtonClasses("secondary")} onClick={handleTestCase} disabled={isRunningTest}>
            <Play className="w-4 h-4 mr-1" />
            Run
          </button>

          <button
            className={getButtonClasses("accent")}
            onClick={handleSubmit}
            disabled={isSubmitting}
          >
            <CheckCircle className="w-4 h-4 mr-1" />
            {isSubmitting ? "Submitting..." : "Submit"}
          </button>
        </div>
      </div>

      <div className="flex-1 relative min-h-[400px]">
        <Editor
          height="100%"
          width="100%"
          language={editorLanguage}
          value={code}
          onChange={(value) => setCode(value || "")}
          theme="vs-dark"
          loading="Loading editor..."
          options={{
            automaticLayout: true,
            selectOnLineNumbers: true,
            fontSize: fontSize,
            fontFamily: "monospace",
          }}
        />
      </div>

      <div className="flex flex-col sm:flex-row gap-4 p-4">
        <div className="flex-1 h-32 border-t border-border bg-secondary p-4 overflow-y-hidden rounded-md">
          <textarea
            onChange={(e) => setTestInput(e.target.value)}
            value={testInput}
            placeholder="INPUT"
            className="w-full h-full resize-none bg-background text-foreground text-sm p-2 rounded-md outline-none"
          />
        </div>

        <div className="flex-1 h-32 border-t border-border bg-secondary p-4 overflow-y-hidden rounded-md">
          <textarea
            value={output}
            placeholder="OUTPUT"
            disabled
            className="w-full h-full resize-none bg-background text-foreground text-sm p-2 rounded-md outline-none"
          />
        </div>

      </div>

    </div>
  );
};

export default EditorPanel;
