import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import Verdict from "@/atoms/Verdict";

/* -------------------------------------------------------------------------- */
/*  The live judge: types a solution, runs 12 tests, stamps a verdict, repeats  */
/* -------------------------------------------------------------------------- */
const TESTS = 12;

const problems = [
  {
    file: "merge.py",
    code: `def merge(intervals):
    # sort by start so overlaps are neighbours
    intervals.sort(key=lambda x: x[0])
    merged = []
    for start, end in intervals:
        if merged and start <= merged[-1][1]:
            merged[-1][1] = max(merged[-1][1], end)
        else:
            merged.append([start, end])
    return merged`,
    ms: 38,
  },
  {
    file: "coins.py",
    code: `def fewest_coins(coins, amount):
    best = [0] + [amount + 1] * amount
    for a in range(1, amount + 1):
        for c in coins:
            if c <= a:
                best[a] = min(best[a], best[a - c] + 1)
    return best[amount] if best[amount] <= amount else -1`,
    ms: 52,
  },
  {
    file: "islands.py",
    code: `def count_islands(grid):
    seen, count = set(), 0
    def walk(r, c):
        if not (0 <= r < len(grid) and 0 <= c < len(grid[0])):
            return
        if (r, c) in seen or grid[r][c] == 0:
            return
        seen.add((r, c))
        for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            walk(r + dr, c + dc)
    for r in range(len(grid)):
        for c in range(len(grid[0])):
            if grid[r][c] and (r, c) not in seen:
                walk(r, c)
                count += 1
    return count`,
    ms: 61,
  },
];

const KEYWORDS = /^(def|return|for|in|if|else|and|or|not|lambda|import)$/;

// Highlights one line with the Proof syntax tokens. Works on partly typed lines too.
const highlight = (line: string) => {
  const hash = line.indexOf("#");
  const code = hash === -1 ? line : line.slice(0, hash);
  const comment = hash === -1 ? "" : line.slice(hash);
  const parts = code.split(/(\b\w+\b)/g).map((tok, i) => {
    if (KEYWORDS.test(tok)) return <span key={i} className="font-medium text-[hsl(var(--syn-keyword))]">{tok}</span>;
    if (/^\d+$/.test(tok)) return <span key={i} className="text-info">{tok}</span>;
    return <span key={i}>{tok}</span>;
  });
  return (
    <>
      {parts}
      {comment && <span className="italic text-muted-foreground">{comment}</span>}
    </>
  );
};

type Phase = "typing" | "testing" | "verdict";

const usePrefersReducedMotion = () => {
  const [reduced, setReduced] = useState(
    () => typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
  useEffect(() => {
    const mq = window.matchMedia("(prefers-reduced-motion: reduce)");
    const update = () => setReduced(mq.matches);
    mq.addEventListener("change", update);
    return () => mq.removeEventListener("change", update);
  }, []);
  return reduced;
};

const LiveJudge = ({ reduced }: { reduced: boolean }) => {
  const [index, setIndex] = useState(0);
  const [typed, setTyped] = useState(reduced ? problems[0].code.length : 0);
  const [passed, setPassed] = useState(reduced ? TESTS : 0);
  const [phase, setPhase] = useState<Phase>(reduced ? "verdict" : "typing");
  const timer = useRef<number>();

  const problem = problems[index];

  useEffect(() => {
    if (reduced) {
      setTyped(problem.code.length);
      setPassed(TESTS);
      setPhase("verdict");
      return;
    }

    const clear = () => window.clearTimeout(timer.current);
    const after = (ms: number, fn: () => void) => {
      timer.current = window.setTimeout(fn, ms);
    };

    if (phase === "typing") {
      if (typed < problem.code.length) {
        const ch = problem.code[typed];
        // a touch of human rhythm: pause after newlines and punctuation
        after(ch === "\n" ? 140 : /[(:,]/.test(ch) ? 55 : 22 + Math.random() * 22, () => setTyped((t) => t + 1));
      } else {
        after(450, () => setPhase("testing"));
      }
    } else if (phase === "testing") {
      if (passed < TESTS) {
        after(130, () => setPassed((p) => p + 1));
      } else {
        after(250, () => setPhase("verdict"));
      }
    } else {
      after(2600, () => {
        setIndex((i) => (i + 1) % problems.length);
        setTyped(0);
        setPassed(0);
        setPhase("typing");
      });
    }
    return clear;
  }, [phase, typed, passed, problem.code, reduced]);

  const visible = problem.code.slice(0, typed);
  const lines = visible.split("\n");

  return (
    <figure
      aria-label="A solution being typed, tested and accepted"
      className="w-full overflow-hidden rounded-md border border-border bg-secondary"
    >
      <div className="flex h-10 items-center justify-between border-b border-border bg-card px-3">
        <span className="font-mono text-[13px] font-medium">{problem.file}</span>
        <div className="h-6">
          {phase === "verdict" ? (
            <span key={`v-${index}`} className="proof-stamp inline-block">
              <Verdict status="PASSED" />
            </span>
          ) : phase === "testing" ? (
            <Verdict status="RUNNING" />
          ) : (
            <span className="font-mono text-xs text-muted-foreground">Editing</span>
          )}
        </div>
      </div>

      <pre className="m-0 h-[354px] overflow-hidden py-3 font-mono text-sm leading-[22px]" aria-hidden="true">
        {lines.map((line, i) => {
          const isLast = i === lines.length - 1;
          return (
            <div
              key={i}
              className={`grid grid-cols-[48px_1fr] whitespace-pre ${isLast && phase === "typing" ? "bg-highlight-wash" : ""}`}
            >
              <b className="select-none pr-3 text-right font-normal text-muted-foreground">{i + 1}</b>
              <span>
                {highlight(line)}
                {isLast && phase === "typing" && <span className="proof-caret" />}
              </span>
            </div>
          );
        })}
      </pre>

      <figcaption className="flex items-center justify-between gap-4 border-t border-border bg-card px-3 py-2">
        <div className="flex gap-1" aria-hidden="true">
          {Array.from({ length: TESTS }, (_, i) => (
            <span
              key={`${index}-${i}`}
              className={`block h-3 w-3 border border-current ${
                i < passed ? "proof-tick bg-success text-success" : "text-rule-strong"
              }`}
            />
          ))}
        </div>
        <span className="font-mono text-xs text-muted-foreground">
          {phase === "verdict" ? (
            <>
              <span className="text-foreground">{TESTS}/{TESTS}</span> tests · <span className="text-foreground">{problem.ms} ms</span>
            </>
          ) : phase === "testing" ? (
            <>
              <span className="text-foreground">{passed}/{TESTS}</span> tests
            </>
          ) : (
            "not run yet"
          )}
        </span>
      </figcaption>
    </figure>
  );
};

/* -------------------------------------------------------------------------- */
/*  Ghost tokens drifting behind the page, nudged by the pointer              */
/* -------------------------------------------------------------------------- */
const ghosts = [
  { t: "O(n log n)", x: 6, y: 14, d: 0 },
  { t: "dp[i][j]", x: 22, y: 78, d: 2 },
  { t: "while lo < hi:", x: 40, y: 8, d: 4 },
  { t: "heapq.heappop", x: 58, y: 86, d: 1 },
  { t: "→ BFS", x: 74, y: 12, d: 3 },
  { t: "mod 10**9 + 7", x: 86, y: 70, d: 5 },
  { t: "[[1, 3], [2, 6]]", x: 12, y: 52, d: 6 },
  { t: "return -1", x: 92, y: 36, d: 2.5 },
  { t: "n ≤ 10⁵", x: 32, y: 92, d: 4.5 },
  { t: "visited = set()", x: 66, y: 48, d: 1.5 },
];

const GhostField = ({ reduced }: { reduced: boolean }) => {
  const [m, setM] = useState({ x: 0, y: 0 });

  useEffect(() => {
    if (reduced) return;
    const onMove = (e: MouseEvent) =>
      setM({ x: e.clientX / window.innerWidth - 0.5, y: e.clientY / window.innerHeight - 0.5 });
    window.addEventListener("mousemove", onMove);
    return () => window.removeEventListener("mousemove", onMove);
  }, [reduced]);

  return (
    <div
      aria-hidden="true"
      className="pointer-events-none absolute inset-0 overflow-hidden"
      style={{ transform: `translate(${m.x * -18}px, ${m.y * -12}px)`, transition: "transform 500ms cubic-bezier(0.22,1,0.36,1)" }}
    >
      {ghosts.map((g) => (
        <span
          key={g.t}
          className="proof-drift absolute whitespace-nowrap font-mono text-sm text-muted-foreground opacity-25"
          style={{ left: `${g.x}%`, top: `${g.y}%`, animationDelay: `-${g.d * 2}s` }}
        >
          {g.t}
        </span>
      ))}
    </div>
  );
};

/* -------------------------------------------------------------------------- */
const Home = () => {
  const reduced = usePrefersReducedMotion();

  return (
    <main className="relative flex min-h-[calc(100vh-3.5rem)] items-center overflow-hidden">
      <GhostField reduced={reduced} />

      <div className="relative z-10 mx-auto grid w-full max-w-[1600px] items-center gap-12 px-6 py-12 lg:grid-cols-[minmax(0,6fr)_minmax(0,5fr)] lg:gap-16">
        <div>
          <h1 className="font-serif font-medium leading-[0.98] tracking-[-0.03em] text-[clamp(3.5rem,9vw,9rem)]">
            <span className="proof-rise block" style={{ animationDelay: "0.05s" }}>
              Solve it.
            </span>
            <span className="proof-rise block" style={{ animationDelay: "0.3s" }}>
              Then{" "}
              <span className="proof-sweep box-decoration-clone px-[0.12em]">prove it.</span>
            </span>
          </h1>

          <div className="proof-rise mt-10 flex flex-wrap gap-3" style={{ animationDelay: "1.2s" }}>
            <Link
              to="/explore"
              className="inline-flex h-11 items-center rounded-sm border border-highlight bg-highlight px-6 text-[15px] font-semibold text-highlight-foreground transition-colors hover:border-primary hover:bg-primary hover:text-primary-foreground"
            >
              Start solving
            </Link>
            <Link
              to="/register"
              className="inline-flex h-11 items-center rounded-sm border border-input px-6 text-[15px] font-semibold transition-colors hover:border-foreground hover:bg-highlight-wash"
            >
              Create an account
            </Link>
          </div>
        </div>

        <div className="proof-rise" style={{ animationDelay: "0.9s" }}>
          <LiveJudge reduced={reduced} />
        </div>
      </div>
    </main>
  );
};

export default Home;
