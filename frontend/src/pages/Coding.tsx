import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import EditorPanel from "@/molecules/EditorPanel";
import ProblemPanel from "@/molecules/ProblemPanel";
import { Problem } from "@/types/problem";
import { fetchProblem } from "@/service/codingService";
import CodingLoading from "@/atoms/CodifingLoading";
import CodingError from "@/atoms/CodingError";
import CodingProblemNotFound from "@/atoms/CodingProblemNotFound";
import type { Tab } from "@/types/Tabs";
import AssistantSidebar from "@/components/ui/AssistantSidebar";

const Coding = () => {
  const { id } = useParams<{ id: string }>();
  const [problem, setProblem] = useState<Problem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [code, setCode] = useState<string | "">("");
  const [problemId, setProblemId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>('problem');
  const [submissionId, setSubmissionId] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    fetchProblem(
      setLoading,
      setError,
      setProblem,
      id,
    );
  }, [id]);

  console.log(problem);

  useEffect(() => {
    if (problem) {
      setProblemId(problem.id);
    }
  }, [problem]);


  if (loading) { return <CodingLoading />; }
  if (error) { return <CodingError error={error} />; }
  if (!problem) { return <CodingProblemNotFound />; }

  return (
    <div className="relative flex w-full h-[calc(100vh-4rem)] overflow-y-auto lg:overflow-hidden bg-background"> {/* Scroll on mobile, fixed on desktop */}

      {/* 2. Add the Sidebar */}
      <AssistantSidebar
        problemStatement={problem.description}
        code={code}
        problemId={String(problem.id)}
      />

      <div className="min-h-full w-full flex flex-col lg:flex-row gap-4 p-4 transition-all">
        <ProblemPanel
          problem={problem}
          activeTab={activeTab}
          setActiveTab={setActiveTab}
          submissionId={submissionId}
        />

        <div className="w-full lg:w-3/5 h-[600px] lg:h-full shrink-0">
          <EditorPanel
            code={code}
            setCode={setCode}
            problemId={problemId}
            setAcitveTab={setActiveTab}
            setSubmissionId={setSubmissionId}
          />
        </div>
      </div>
    </div>
  );
};

export default Coding;