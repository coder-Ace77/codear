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

  useEffect(() => {
    if (problem) {
      setProblemId(problem.id);
    }
  }, [problem]);


  if (loading) { return <CodingLoading />; }
  if (error) { return <CodingError error={error} />; }
  if (!problem) { return <CodingProblemNotFound />; }

  return (
    <div className="relative flex h-[calc(100vh-3.5rem)] w-full overflow-y-auto bg-background lg:overflow-hidden">
      <div className="flex min-h-full w-full flex-col lg:h-full lg:min-h-0 lg:grid lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)] lg:grid-rows-[minmax(0,1fr)]">
        <ProblemPanel
          problem={problem}
          activeTab={activeTab}
          setActiveTab={setActiveTab}
          submissionId={submissionId}
        />

        <div className="h-[640px] shrink-0 lg:h-full lg:min-h-0 lg:overflow-y-auto">
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
