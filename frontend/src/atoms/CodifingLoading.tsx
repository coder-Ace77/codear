import { Loader2 } from "lucide-react";

const CodingLoading = () => {
  return (
    <div className="h-[calc(100vh-4rem)] flex items-center justify-center">
      <Loader2 className="w-12 h-12 animate-spin text-primary" />
    </div>
  );
}

export default CodingLoading;