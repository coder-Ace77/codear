import { useEffect, useRef, useState } from "react";

interface Options {
  threshold?: number;
  /** if true (default) the element stays "in view" after the first intersection */
  once?: boolean;
  rootMargin?: string;
}

/**
 * Fires `inView` when the element scrolls into the viewport. Used to drive
 * the landing page's scroll-triggered entrance animations.
 */
export const useInView = <T extends HTMLElement = HTMLDivElement>(opts: Options = {}) => {
  const { threshold = 0.25, once = true, rootMargin = "0px 0px -8% 0px" } = opts;
  const ref = useRef<T>(null);
  const [inView, setInView] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const obs = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setInView(true);
          if (once) obs.disconnect();
        } else if (!once) {
          setInView(false);
        }
      },
      { threshold, rootMargin }
    );
    obs.observe(el);
    return () => obs.disconnect();
  }, [threshold, once, rootMargin]);

  return { ref, inView };
};
