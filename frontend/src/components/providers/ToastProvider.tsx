
import { Toaster } from "react-hot-toast";

const ToastProvider = () => {
  return (
    <Toaster
      position="top-center"
      reverseOrder={false}
      toastOptions={{
        style: {
          background: "#1E1E1E",
          color: "#FAFAFA",
          border: "1px solid #3E3E3E",
        },
        duration: 4000,
      }}
    />
  );
};

export default ToastProvider;