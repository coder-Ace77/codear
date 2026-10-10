import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Navbar from "@/molecules/Navbar";
import Home from "./pages/Home";
import Explore from "./pages/Explore";
import Coding from "./pages/Coding";
import Profile from "./pages/Profile";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Admin from "./pages/Admin";
import Developers from "./pages/Developers";
import NotFound from "./pages/NotFound";
import SubmissionResult from "./pages/SubmissionResult";
import Tests from "./pages/Tests";
import TakeTest from "./pages/TakeTest";
import TestAttempt from "./pages/TestAttempt";
import ToastProvider from "./components/providers/ToastProvider";

import BackendHealthCheck from "@/components/ui/BackendHealthCheck";

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter>
        <div className="min-h-screen bg-background text-foreground">
          <BackendHealthCheck />
          <Navbar />
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/explore" element={<Explore />} />
            <Route path="/coding/:id" element={<Coding />} />
            <Route path="/profile" element={<Profile />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/admin" element={<Admin />} />
            <Route path="/developers" element={<Developers />} />
            <Route path="/tests" element={<Tests />} />
            <Route path="/test/attempt/:attemptId" element={<TestAttempt />} />
            <Route path="/test/:token" element={<TakeTest />} />
            <Route path="/submissions/:submissionId" element={<SubmissionResult />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </div>
        <ToastProvider />
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
