import { useEffect, useState, useRef } from "react"; // 1. Import useRef
import { useParams } from "react-router-dom";
import { codingService } from "@/service/codingService";
import toast from "react-hot-toast";

const FINAL_STATES = ["PASSED", "FAILED", "COMPLETED"];

const SubmissionResult = () => {
  const { submissionId } = useParams();
  const [output, setOutput] = useState("Waiting for result…");
  const [isLoading, setIsLoading] = useState(true);

  const intervalRef = useRef(null);

  useEffect(() => {
    if (!submissionId) return;
    const toastId = toast.loading("Running code");
    const fetchStatus = async () => {
      try {
        const data = await codingService.getSubmissionStatus(submissionId);
        if (FINAL_STATES.includes(data.status.toUpperCase())){
          clearInterval(intervalRef.current);
          setOutput(
            `Status: ${data.status}\nResult: ${data.result}\nPassed: ${data.passedTests}/${data.totalTests}\nTime: ${data.timeTakenMs}ms\nMemory: ${data.memoryUsed}`
          );
          if(data.status=="PASSED"){
            toast.success("All test passed",{id:toastId});
          }else{
            toast.error("Error running tests",{id:toastId});
          }
          setIsLoading(false);
        } else {          
          setOutput(`Status: ${data.status}. Still processing…`);
          setIsLoading(true); 
          toast.loading("Running code",{id:toastId});
        }
      } catch (error) {
        clearInterval(intervalRef.current);
        setOutput("Failed to fetch the submission result.");
      } finally {
        setIsLoading(false);
        toast.error("Failed to get result",{id:toastId});
      }
    };
    fetchStatus();
    intervalRef.current = setInterval(fetchStatus, 1000); 
    return () => clearInterval(intervalRef.current);
  }, [submissionId]); 

  return (
    <main className="mx-auto max-w-2xl px-6 py-12">
      <h1 className="mb-4 font-serif text-[32px] font-medium leading-[38px] tracking-[-0.01em]">
        Submission result
      </h1>

      {isLoading && <p className="mb-3 text-muted-foreground">Fetching result…</p>}

      <pre className="whitespace-pre-wrap rounded-md border border-border bg-secondary p-4 font-mono text-[13px] leading-5">
        {output}
      </pre>
    </main>
  );
};

export default SubmissionResult;
