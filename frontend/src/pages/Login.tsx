import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Button from "@/atoms/Button";
import Input from "@/atoms/Input";
import Label from "@/atoms/Label";
import AuthLayout from "@/molecules/AuthLayout";
import apiClient from "@/lib/apiClient";
import toast from "react-hot-toast";

const Login = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const navigate = useNavigate();
  // only ever go to a path on this site, never an address that arrived in the URL
  const next = new URLSearchParams(window.location.search).get("next");
  const destination = next && /^\/(?!\/)[\w\-./]*$/.test(next) ? next : "/profile";

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const response = await apiClient.post("/user/login", {
        email: email,
        password: password,
      });

      if (response && response.data && response.data.token) {
        localStorage.setItem("token", response.data.token);
        toast.success("Signed in");
        navigate(destination);
      } else {
        toast.error("Sign in failed. Check your email and password.");
      }
    } catch (e: any) {
      // The API reports the reason as `detail` (FastAPI) or `message`; 5xx responses carry neither.
      const data = e.response?.data;
      const reason = typeof data?.detail === "string" ? data.detail : data?.message;
      const errorMessage = reason
        ? reason
        : e.response?.status >= 500
          ? "The server had a problem. Try again in a moment."
          : e.request && !e.response
            ? "No response from the server. Check your connection."
            : "Sign in failed. Try again.";

      toast.error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Sign in"
      subtitle="Pick up where you stopped."
      footer={
        <>
          Don't have an account?{" "}
          <Link to="/register" className="font-medium text-foreground underline underline-offset-[3px]">
            Sign up
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <Label htmlFor="email">Email</Label>
          <Input
            id="email"
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            disabled={isLoading}
          />
        </div>

        <div>
          <Label htmlFor="password">Password</Label>
          <Input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            disabled={isLoading}
          />
        </div>

        <Button type="submit" variant="primary" className="w-full" disabled={isLoading}>
          {isLoading ? "Signing in…" : "Sign in"}
        </Button>
      </form>
    </AuthLayout>
  );
};

export default Login;
