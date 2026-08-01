import { AlertTriangle } from "lucide-react";
const CodingError =({error})=>{
    return (
      <div className="h-[calc(100vh-4rem)] flex flex-col items-center justify-center text-destructive">
        <AlertTriangle className="w-12 h-12 mb-4" />
        <h2 className="text-xl font-semibold">Error</h2>
        <p>{error}</p>
      </div>
    );
}

export default CodingError;