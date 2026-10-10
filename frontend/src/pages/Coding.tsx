import { useState, useEffect } from "react";
import { useParams, useSearchParams } from "react-router-dom";
import TestBanner, { TEST_BANNER_HEIGHT } from "@/molecules/TestBanner";
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
  const attemptParam = Number(useSearchParams()[0].get("attempt"));
  const attemptId = Number.isInteger(attemptParam) && attemptParam > 0 ? attemptParam : null;
  const [problem, setProblem] = useState<Problem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [code, setCode] = useState<string | "">("");
  const [problemId, setProblemId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>('problem');
  const [submissionId, setSubmissionId] = useState<string | null>(null);
  const [submissionsVersion, setSubmissionsVersion] = useState(0);

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
    <>
    {attemptId !== null && <TestBanner attemptId={attemptId} />}
    <div
      className="relative flex w-full overflow-y-auto bg-background lg:overflow-hidden"
      style={{ height: `calc(100vh - 3.5rem - ${attemptId !== null ? TEST_BANNER_HEIGHT : "0rem"})` }}
    >
      <div className="flex min-h-full w-full flex-col lg:h-full lg:min-h-0 lg:grid lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)] lg:grid-rows-[minmax(0,1fr)]">
        <ProblemPanel
          problem={problem}
          activeTab={activeTab}
          testMode={attemptId !== null}
          setActiveTab={setActiveTab}
          submissionId={submissionId}
          submissionsVersion={submissionsVersion}
        />

        <div className="h-[640px] shrink-0 lg:h-full lg:min-h-0 lg:overflow-y-auto">
          <EditorPanel
            code={code}
            setCode={setCode}
            problemId={problemId}
            setAcitveTab={setActiveTab}
            setSubmissionId={setSubmissionId}
            onSubmissionFinished={() => setSubmissionsVersion((v) => v + 1)}
            attemptId={attemptId}
          />
        </div>
      </div>
    </div>
    </>
  );
};

export default Coding;
