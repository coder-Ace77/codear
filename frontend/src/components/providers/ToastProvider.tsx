
import { Toaster } from "react-hot-toast";

const ToastProvider = () => {
  return (
    <Toaster
      position="top-center"
      reverseOrder={false}
      toastOptions={{
        style: {
          background: "hsl(var(--paper-raised))",
          color: "hsl(var(--ink))",
          border: "1px solid hsl(var(--rule-strong))",
          borderRadius: "6px",
          fontSize: "14px",
          boxShadow: "var(--shadow-pop)",
        },
        duration: 4000,
      }}
    />
  );
};

export default ToastProvider;