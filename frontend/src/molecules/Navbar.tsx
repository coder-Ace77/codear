import { Link, useLocation, useNavigate } from "react-router-dom";
import { Menu, X, Moon, Sun } from "lucide-react";
import { useState, useEffect, useMemo } from "react";
import { cn } from "@/lib/utils";
import { User } from "@/types/User";
import apiClient from "@/lib/apiClient";
import toast from "react-hot-toast";
import { useTheme } from "@/hooks/useTheme";

// Only the role decides. The backend re-checks it on every admin request; this just hides the link.
const isAdmin = (user: User | null) => !!user && user.role === "ADMIN";

const initials = (name: string) =>
  name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? "")
    .join("");

const Navbar = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const { toggle } = useTheme();

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const response = await apiClient.get("/user/user");
        setUser(response.data as User);
      } catch (e) {
        if (e.response?.status !== 401 && e.response?.status !== 403) {
          console.error("Unable to load user", e);
        }
        setUser(null);
      }
    };
    fetchUser();
  }, [location.pathname]);

  const navLinks = useMemo(() => {
    const links = [
      { path: "/", label: "Home" },
      { path: "/explore", label: "Problems" },
      { path: "/developers", label: "API" },
    ];
    if (isAdmin(user)) links.push({ path: "/admin", label: "Admin" });
    return links;
  }, [user]);

  const isActive = (path: string) =>
    path === "/" ? location.pathname === "/" : location.pathname.startsWith(path);

  const handleLogout = () => {
    localStorage.removeItem("token");
    delete apiClient.defaults.headers.common["Authorization"];
    setUser(null);
    setMobileMenuOpen(false);
    toast.success("Logged out");
    navigate("/");
  };

  const linkClass = (path: string) =>
    cn(
      "py-1 text-sm font-medium transition-colors",
      isActive(path)
        ? "text-foreground shadow-[0_2px_0_hsl(var(--ink))]"
        : "text-muted-foreground hover:text-foreground"
    );

  const iconButton =
    "flex h-8 w-8 items-center justify-center rounded-sm border border-input text-foreground transition-colors hover:bg-highlight-wash";

  const themeToggle = (
    <button onClick={toggle} aria-label="Toggle light and dark theme" className={iconButton}>
      <Moon className="h-4 w-4 dark-hidden" aria-hidden="true" />
      <Sun className="h-4 w-4 light-hidden" aria-hidden="true" />
    </button>
  );

  const streak =
    user?.dailyStreak && user.dailyStreak > 0 ? (
      <span className="mark-fill px-2 py-0.5 font-mono text-xs font-medium">
        {user.dailyStreak}-day streak
      </span>
    ) : null;

  return (
    <nav className="sticky top-0 z-50 border-b border-border bg-background">
      <div className="mx-auto flex h-14 max-w-[1600px] items-center gap-6 px-6">
        <Link to="/" className="font-serif text-[26px] font-medium leading-none tracking-tight text-foreground">
          Code Arena
        </Link>

        <div className="hidden flex-1 items-center gap-6 md:flex">
          {navLinks.map((link) => (
            <Link
              key={link.path}
              to={link.path}
              aria-current={isActive(link.path) ? "page" : undefined}
              className={linkClass(link.path)}
            >
              {link.label}
            </Link>
          ))}
        </div>

        <div className="ml-auto hidden items-center gap-3 md:flex">
          {streak}
          {themeToggle}
          {user ? (
            <>
              <Link
                to="/profile"
                aria-label="Account"
                title={user.name || user.username}
                className="grid h-8 w-8 place-items-center rounded-full border border-input text-xs font-semibold text-foreground hover:bg-highlight-wash"
              >
                {initials(user.name || user.username)}
              </Link>
              <button
                onClick={handleLogout}
                className="text-sm font-medium text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
              >
                Log out
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="text-sm font-medium text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
              >
                Sign in
              </Link>
              <Link
                to="/register"
                className="inline-flex h-8 items-center rounded-sm border border-primary bg-primary px-4 text-[13px] font-semibold text-primary-foreground transition-colors hover:border-highlight hover:bg-highlight hover:text-highlight-foreground"
              >
                Sign up
              </Link>
            </>
          )}
        </div>

        <button
          className="ml-auto md:hidden"
          onClick={() => setMobileMenuOpen((v) => !v)}
          aria-label="Menu"
          aria-expanded={mobileMenuOpen}
        >
          {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </div>

      {mobileMenuOpen && (
        <div className="flex flex-col gap-4 border-t border-border bg-background px-6 py-5 md:hidden">
          {navLinks.map((link) => (
            <Link
              key={link.path}
              to={link.path}
              onClick={() => setMobileMenuOpen(false)}
              aria-current={isActive(link.path) ? "page" : undefined}
              className={cn("text-sm font-medium", isActive(link.path) ? "text-foreground" : "text-muted-foreground")}
            >
              {link.label}
            </Link>
          ))}

          <div className="flex flex-col gap-4 border-t border-border pt-4">
            <div className="flex items-center gap-3">
              {themeToggle}
              {streak}
            </div>
            {user ? (
              <>
                <Link to="/profile" onClick={() => setMobileMenuOpen(false)} className="text-sm font-medium">
                  {user.name || user.username}
                </Link>
                <button onClick={handleLogout} className="text-left text-sm font-medium text-muted-foreground">
                  Log out
                </button>
              </>
            ) : (
              <>
                <Link to="/login" onClick={() => setMobileMenuOpen(false)} className="text-sm font-medium">
                  Sign in
                </Link>
                <Link
                  to="/register"
                  onClick={() => setMobileMenuOpen(false)}
                  className="inline-flex h-9 items-center justify-center rounded-sm border border-primary bg-primary text-sm font-semibold text-primary-foreground"
                >
                  Sign up
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </nav>
  );
};

export default Navbar;
