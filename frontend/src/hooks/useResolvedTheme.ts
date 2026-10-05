import { useEffect, useState } from "react";

const resolve = (): "light" | "dark" => {
  const explicit = document.documentElement.getAttribute("data-theme");
  if (explicit === "light" || explicit === "dark") return explicit;
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
};

// The theme actually in effect (explicit choice, else the system setting), kept live for non-CSS consumers like Monaco.
export const useResolvedTheme = () => {
  const [theme, setTheme] = useState<"light" | "dark">(resolve);

  useEffect(() => {
    const update = () => setTheme(resolve());
    const observer = new MutationObserver(update);
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ["data-theme"] });
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    media.addEventListener("change", update);
    return () => {
      observer.disconnect();
      media.removeEventListener("change", update);
    };
  }, []);

  return theme;
};
