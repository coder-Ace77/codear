import { Link, useLocation } from "react-router-dom";
import { useEffect } from "react";

const NotFound = () => {
  const location = useLocation();

  useEffect(() => {
    console.error("404 Error: User attempted to access non-existent route:", location.pathname);
  }, [location.pathname]);

  return (
    <main className="mx-auto flex min-h-[calc(100vh-3.5rem)] max-w-md flex-col justify-center px-6 text-center">
      <p className="font-mono text-[13px] text-muted-foreground">404</p>
      <h1 className="mt-1 font-serif text-[32px] font-medium leading-[38px]">Page not found</h1>
      <p className="mb-6 mt-2 text-muted-foreground">There is nothing at this address.</p>
      <Link to="/" className="font-medium text-foreground underline underline-offset-[3px]">
        Return home
      </Link>
    </main>
  );
};

export default NotFound;
