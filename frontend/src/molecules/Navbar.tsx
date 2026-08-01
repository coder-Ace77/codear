import { Link, useLocation, useNavigate } from "react-router-dom";
import { Menu, X, LogOut } from "lucide-react";
import { useState, useEffect, useMemo } from "react";
import { cn } from "@/lib/utils";
import { User } from "@/types/User";
import apiClient from "@/lib/apiClient";
import toast from "react-hot-toast";

// ── Editorial palette (shared with the landing page) ──
const C = {
  bg: "#2a2620",
  deep: "#221e19",
  gold: "#b39a6d",
  goldBright: "#c6ad7c",
  cream: "#c8b48d",
  ink: "#26221c",
  muted: "#7d6f4e",
};

const mono = "font-['Space_Mono']";

const isAdmin = (user: User | null) =>
  !!user && (user.role === "ADMIN" || user.username === "Admin");

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
      { path: "/", label: "HOME" },
      { path: "/explore", label: "PROBLEMS" },
    ];
    if (isAdmin(user)) links.push({ path: "/admin", label: "ADMIN" });
    return links;
  }, [user]);

  const isActive = (path: string) =>
    path === "/" ? location.pathname === "/" : location.pathname.startsWith(path);

  const handleLogout = () => {
    localStorage.removeItem("token");
    delete apiClient.defaults.headers.common["Authorization"];
    setUser(null);
    setMobileMenuOpen(false);
    toast.success("Logged out successfully");
    navigate("/");
  };

  const navLinkClass = (path: string) =>
    cn(
      `${mono} text-[11px] tracking-[0.25em] transition-opacity pb-1 border-b`,
      isActive(path) ? "opacity-100" : "opacity-60 hover:opacity-100"
    );

  return (
    <nav
      className="sticky top-0 z-50 border-b backdrop-blur-md"
      style={{ backgroundColor: `${C.bg}f2`, borderColor: "rgba(200,180,141,0.15)" }}
    >
      <div className="mx-auto max-w-[1600px] px-6 md:px-10 h-20 flex items-center justify-between">
        {/* wordmark */}
        <Link
          to="/"
          className={`${mono} font-bold text-lg md:text-xl tracking-[0.2em]`}
          style={{ color: C.cream }}
        >
          CODE ARENA
        </Link>

        {/* center nav links */}
        <div className="hidden md:flex items-center gap-10">
          {navLinks.map((link) => (
            <Link
              key={link.path}
              to={link.path}
              className={navLinkClass(link.path)}
              style={{ color: C.cream, borderColor: isActive(link.path) ? C.gold : "transparent" }}
            >
              {link.label}
            </Link>
          ))}
        </div>

        {/* right auth */}
        <div className="hidden md:flex items-center gap-4">
          {user ? (
            <>
              <Link
                to="/profile"
                className="flex items-center gap-3 rounded-full pl-1.5 pr-5 py-1.5 border transition-colors hover:bg-white/5"
                style={{ borderColor: C.muted }}
              >
                <span
                  className={`${mono} text-xs font-bold w-8 h-8 rounded-full flex items-center justify-center`}
                  style={{ backgroundColor: C.cream, color: C.ink }}
                >
                  {initials(user.name || user.username)}
                </span>
                <span className={`${mono} text-[11px] tracking-[0.15em]`} style={{ color: C.cream }}>
                  {user.name || user.username}
                </span>
              </Link>
              <button
                onClick={handleLogout}
                aria-label="Logout"
                className="w-10 h-10 rounded-full border flex items-center justify-center transition-colors hover:bg-white/5"
                style={{ borderColor: C.muted }}
              >
                <LogOut className="w-4 h-4" style={{ color: C.cream }} />
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className={`${mono} text-[11px] tracking-[0.25em] transition-opacity hover:opacity-60`}
                style={{ color: C.cream }}
              >
                SIGN IN
              </Link>
              <Link
                to="/register"
                className={`${mono} text-[11px] tracking-[0.2em] rounded-full px-6 py-3 transition-transform hover:scale-[1.03]`}
                style={{ backgroundColor: C.cream, color: C.ink }}
              >
                SIGN UP
              </Link>
            </>
          )}
        </div>

        {/* mobile toggle */}
        <button
          className="md:hidden"
          onClick={() => setMobileMenuOpen((v) => !v)}
          aria-label="Menu"
          style={{ color: C.cream }}
        >
          {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
        </button>
      </div>

      {/* mobile menu */}
      {mobileMenuOpen && (
        <div
          className="md:hidden border-t px-6 py-6 flex flex-col gap-5"
          style={{ backgroundColor: C.deep, borderColor: "rgba(200,180,141,0.15)" }}
        >
          {navLinks.map((link) => (
            <Link
              key={link.path}
              to={link.path}
              onClick={() => setMobileMenuOpen(false)}
              className={`${mono} text-xs tracking-[0.25em]`}
              style={{ color: C.cream, opacity: isActive(link.path) ? 1 : 0.6 }}
            >
              {link.label}
            </Link>
          ))}

          <div className="pt-4 border-t" style={{ borderColor: "rgba(200,180,141,0.15)" }}>
            {user ? (
              <div className="flex flex-col gap-4">
                <Link
                  to="/profile"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center gap-3"
                >
                  <span
                    className={`${mono} text-xs font-bold w-8 h-8 rounded-full flex items-center justify-center`}
                    style={{ backgroundColor: C.cream, color: C.ink }}
                  >
                    {initials(user.name || user.username)}
                  </span>
                  <span className={`${mono} text-xs tracking-[0.15em]`} style={{ color: C.cream }}>
                    {user.name || user.username}
                  </span>
                </Link>
                <button
                  onClick={handleLogout}
                  className={`${mono} text-xs tracking-[0.25em] flex items-center gap-2`}
                  style={{ color: C.cream }}
                >
                  <LogOut className="w-4 h-4" /> LOGOUT
                </button>
              </div>
            ) : (
              <div className="flex flex-col gap-4">
                <Link
                  to="/login"
                  onClick={() => setMobileMenuOpen(false)}
                  className={`${mono} text-xs tracking-[0.25em]`}
                  style={{ color: C.cream }}
                >
                  SIGN IN
                </Link>
                <Link
                  to="/register"
                  onClick={() => setMobileMenuOpen(false)}
                  className={`${mono} text-xs tracking-[0.2em] rounded-full px-6 py-3 text-center`}
                  style={{ backgroundColor: C.cream, color: C.ink }}
                >
                  SIGN UP
                </Link>
              </div>
            )}
          </div>
        </div>
      )}
    </nav>
  );
};

export default Navbar;
