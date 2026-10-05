import { useState } from "react";
import { Problem } from "@/types/problem";
import EditorialTab from "./EditorialTab";
import { Tab } from "@/types/Tabs";
import ProblemDescription from "./ProblemDescription";
import { SubmissionsContent } from "./SubmissionContent";

const EditorialContent = () => (
  <div className="p-6">
    <h2 className="mb-3 font-serif text-2xl font-medium">Editorial</h2>
    <p className="text-muted-foreground">The editorial for this problem is not yet available.</p>
  </div>
);

interface ProblemPanelProps {
  problem: Problem;
  activeTab: Tab;
  setActiveTab: React.Dispatch<React.SetStateAction<Tab>>;
  submissionId: string | null;
}
const ProblemPanel: React.FC<ProblemPanelProps> = ({ problem, activeTab, setActiveTab, submissionId }) => {

  const renderTabContent = () => {
    switch (activeTab) {
      case "problem":
        return <ProblemDescription problem={problem} />;
      case "submissions":
        return <SubmissionsContent problemId={problem.id} />;
      case "editorial":
        return <EditorialTab problemId={problem.id} />;
      default:
        return null;
    }
  };

  const TabButton: React.FC<{ tabId: Tab; label: string }> = ({ tabId, label }) => (
    <button
      role="tab"
      aria-selected={activeTab === tabId}
      onClick={() => setActiveTab(tabId)}
      className="py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground aria-selected:text-foreground aria-selected:shadow-[inset_0_-2px_0_hsl(var(--ink))]"
    >
      {label}
    </button>
  );

  return (
    <div className="flex h-[560px] w-full shrink-0 flex-col border-b border-border lg:h-full lg:min-h-0 lg:border-b-0 lg:border-r">
      <div role="tablist" className="flex gap-6 border-b border-border px-6 pt-4">
        <TabButton tabId="problem" label="Statement" />
        <TabButton tabId="editorial" label="Editorial" />
        <TabButton tabId="submissions" label="Submissions" />
      </div>
      <div className="min-h-0 flex-1 overflow-y-auto">
        {renderTabContent()}
      </div>
    </div>
  );
};

export default ProblemPanel;
