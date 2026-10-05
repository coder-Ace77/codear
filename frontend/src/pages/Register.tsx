import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Button from "@/atoms/Button";
import Input from "@/atoms/Input";
import Label from "@/atoms/Label";
import AuthLayout from "@/molecules/AuthLayout";
import apiClient from "@/lib/apiClient";
import toast from "react-hot-toast";

const Register = () => {
  const [formData, setFormData] = useState({
    name: "",
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
  });

  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await apiClient.post("/user/register", {
        username: formData.username,
        name: formData.name,
        email: formData.email,
        password: formData.password,
      });

      if (res.status === 200 || res.status === 201) {
        toast.success("Account created. Sign in to continue.");
        navigate("/login");
      } else {
        toast.error("Unexpected response. Please try again.");
      }
    } catch (err: any) {
      if (err.response) {
        const detail = err.response.data?.detail;
        toast.error(typeof detail === "string" ? detail : "Could not create the account.");
      } else if (err.request) {
        toast.error("No response from the server. Check your connection.");
      } else {
        toast.error("Unexpected error");
      }
    }
  };

  const handleChange = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData(prev => ({ ...prev, [field]: e.target.value }));
  };

  return (
    <AuthLayout
      title="Create an account"
      subtitle="Solve problems, keep a streak, read the editorials."
      footer={
        <>
          Already have an account?{" "}
          <Link to="/login" className="font-medium text-foreground underline underline-offset-[3px]">
            Sign in
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <Label htmlFor="name">Full name</Label>
          <Input id="name" type="text" placeholder="John Doe" value={formData.name} onChange={handleChange("name")} required />
        </div>

        <div>
          <Label htmlFor="username">Username</Label>
          <Input id="username" type="text" placeholder="johndoe" value={formData.username} onChange={handleChange("username")} required />
        </div>

        <div>
          <Label htmlFor="email">Email</Label>
          <Input id="email" type="email" placeholder="you@example.com" value={formData.email} onChange={handleChange("email")} required />
        </div>

        <div>
          <Label htmlFor="password">Password</Label>
          <Input id="password" type="password" value={formData.password} onChange={handleChange("password")} required />
        </div>

        <div>
          <Label htmlFor="confirmPassword">Confirm password</Label>
          <Input id="confirmPassword" type="password" value={formData.confirmPassword} onChange={handleChange("confirmPassword")} required />
        </div>

        <Button type="submit" variant="primary" className="mt-2 w-full">
          Create account
        </Button>
      </form>
    </AuthLayout>
  );
};

export default Register;
