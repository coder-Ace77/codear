import { ReactNode } from "react";
import { Link } from "react-router-dom";

const AuthLayout = ({
  title,
  subtitle,
  footer,
  children,
}: {
  title: string;
  subtitle: string;
  footer: ReactNode;
  children: ReactNode;
}) => (
  <main className="mx-auto flex min-h-[calc(100vh-3.5rem)] max-w-md flex-col justify-center px-6 py-12">
    <h1 className="font-serif text-[32px] font-medium leading-[38px] tracking-[-0.01em]">{title}</h1>
    <p className="mb-6 mt-1 text-muted-foreground">{subtitle}</p>
    {children}
    <p className="mt-6 text-sm text-muted-foreground">{footer}</p>
    <p className="mt-8 text-[13px] text-muted-foreground">
      <Link to="/" className="underline underline-offset-[3px] hover:text-foreground">
        Back to home
      </Link>
    </p>
  </main>
);

export default AuthLayout;
