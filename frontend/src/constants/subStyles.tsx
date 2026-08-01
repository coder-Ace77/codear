import {
  CheckCircle,
  XCircle,
  Clock,
  Loader2,
  ChevronDown,
  Code,
  LineChart,
  Database,
  FileText,
} from "lucide-react";

import type { Submission, SubmissionStatus } from "@/types/submission"; // Assumes types are in this file


export const getStatusStyles = (status: SubmissionStatus) => {
  switch (status) {
    case "PASSED":
    case "COMPLETED":
      return {
        icon: <CheckCircle className="w-4 h-4" />,
        color: "text-brand-olive",
        bgColor: "bg-brand-olive/10",
      };
    case "FAILED":
    case "ERROR":
      return {
        icon: <XCircle className="w-4 h-4" />,
        color: "text-brand-rust",
        bgColor: "bg-brand-rust/10",
      };
    case "RUNNING":
      return {
        icon: <Loader2 className="w-4 h-4 animate-spin" />,
        color: "text-brand-clay",
        bgColor: "bg-brand-clay/10",
      };
    case "PENDING":
    default:
      return {
        icon: <Clock className="w-4 h-4" />,
        color: "text-brand-yellow",
        bgColor: "bg-brand-yellow/10",
      };
  }
};
