import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowUpRight, Github, Twitter, Linkedin } from "lucide-react";
import { Waveform, Barcode } from "@/molecules/LandingDecor";
import { Reveal, SplitWords } from "@/molecules/Reveal";
import { useInView } from "@/hooks/useInView";

// ── Editorial landing palette (scoped to this page so the rest of the app
//    keeps its current theme while we transform things phase by phase) ──
const C = {
  bg: "#2a2620",
  deep: "#221e19",
  gold: "#b39a6d",
  goldBright: "#c6ad7c",
  cream: "#c8b48d",
  ink: "#26221c",
  muted: "#7d6f4e",
  // warm secondary palette
  orange: "#d3743a",
  yellow: "#e3b24d",
  olive: "#8b8e4f",
  rust: "#ca5f42",
  clay: "#bd8a5a",
};

const serif = "font-['Cormorant_Garamond']";
const mono = "font-['Space_Mono']";

/* -------------------------------------------------------------------------- */
/*  CodeA — brand badge with a multi-stage "draw + settle" entrance and        */
/*  ambient motion (rotating inner ring, floating, pulsing halo).             */
/* -------------------------------------------------------------------------- */
const R = 94;
const CIRC = 2 * Math.PI * R;
const INNER = R - 12;
const INNER_CIRC = 2 * Math.PI * INNER;

const CodeABadge = () => {
  const { ref, inView } = useInView<HTMLDivElement>({ threshold: 0.45 });

  return (
    <div ref={ref} className="relative flex items-center justify-center py-6 md:py-10">
      <div
        className={`relative w-[78%] max-w-[360px] aspect-square flex items-center justify-center ${
          inView ? "animate-float-y" : ""
        }`}
      >
        {/* pulsing halo, appears once the ring has landed */}
        <span
          className={`absolute inset-[8%] rounded-full ${inView ? "animate-ping-ring" : ""}`}
          style={{
            border: `1px solid ${C.muted}`,
            opacity: inView ? undefined : 0,
            animationDelay: "1.4s",
          }}
        />

        <svg
          viewBox="0 0 200 200"
          className="absolute inset-0 w-full h-full"
          style={{
            transform: inView ? "scale(1) rotate(0deg)" : "scale(0.35) rotate(-150deg)",
            opacity: inView ? 1 : 0,
            transition: "transform 1.1s cubic-bezier(0.22,1,0.36,1), opacity 0.9s ease",
          }}
        >
          {/* outer ring — draws in */}
          <circle
            cx="100"
            cy="100"
            r={R}
            fill="none"
            stroke={C.ink}
            strokeWidth="1.5"
            strokeLinecap="round"
            style={{
              strokeDasharray: CIRC,
              strokeDashoffset: inView ? 0 : CIRC,
              transition: "stroke-dashoffset 1.4s cubic-bezier(0.65,0,0.35,1) 0.15s",
            }}
          />
          {/* inner dashed ring — draws, then spins forever */}
          <g
            className={inView ? "animate-spin-slow" : ""}
            style={{ transformOrigin: "100px 100px" }}
          >
            <circle
              cx="100"
              cy="100"
              r={INNER}
              fill="none"
              stroke={C.olive}
              strokeWidth="0.75"
              strokeDasharray="2 6"
              style={{
                strokeDashoffset: inView ? 0 : INNER_CIRC,
                opacity: inView ? 0.85 : 0,
                transition: "stroke-dashoffset 1.5s cubic-bezier(0.65,0,0.35,1) 0.4s, opacity 0.6s ease 0.4s",
              }}
            />
          </g>
          {/* orbiting dot */}
          <g
            className={inView ? "animate-spin-slow" : ""}
            style={{ transformOrigin: "100px 100px", opacity: inView ? 1 : 0, transition: "opacity 0.6s ease 1.2s" }}
          >
            <circle cx="100" cy={100 - R} r="3.5" fill={C.rust} />
          </g>
        </svg>

        {/* wordmark — pops into place after the ring lands */}
        <span
          className={`${serif} relative font-semibold leading-none text-[4.5rem] md:text-[6rem]`}
          style={{
            color: C.ink,
            opacity: inView ? 1 : 0,
            transform: inView ? "scale(1) translateY(0)" : "scale(0.5) translateY(12px)",
            transition:
              "opacity 0.7s ease 0.85s, transform 0.9s cubic-bezier(0.34,1.56,0.64,1) 0.85s",
          }}
        >
          Code<span style={{ color: C.orange }}>A</span>
        </span>
      </div>
    </div>
  );
};

/* -------------------------------------------------------------------------- */
/*  Hero — word-unmasking headline + mouse parallax                            */
/* -------------------------------------------------------------------------- */
const Hero = () => {
  const [m, setM] = useState({ x: 0, y: 0 });

  useEffect(() => {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const onMove = (e: MouseEvent) =>
      setM({ x: e.clientX / window.innerWidth - 0.5, y: e.clientY / window.innerHeight - 0.5 });
    window.addEventListener("mousemove", onMove);
    return () => window.removeEventListener("mousemove", onMove);
  }, []);

  return (
    <section
      className="relative min-h-[calc(100vh-5rem)] overflow-hidden"
      style={{ backgroundColor: C.bg }}
    >
      {/* right-side waveform ridge (breathes + parallax) */}
      <div
        className="absolute inset-y-0 right-0 w-[55%] md:w-[42%] opacity-90"
        style={{
          transform: `translate(${m.x * -26}px, ${m.y * -18}px)`,
          transition: "transform 400ms cubic-bezier(0.22,1,0.36,1)",
        }}
      >
        <Waveform bars={160} seed={11} rise={2.4} color={C.cream} breathe className="h-full" />
      </div>
      <div
        className="absolute inset-0"
        style={{
          background: `linear-gradient(90deg, ${C.bg} 30%, rgba(42,38,32,0.4) 60%, rgba(42,38,32,0) 100%)`,
        }}
      />

      <div className="relative z-10 mx-auto max-w-[1600px] px-6 md:px-10 min-h-[calc(100vh-5rem)] flex items-center">
        <h1
          className={`${serif} uppercase leading-[0.95] max-w-[14ch] text-[13vw] md:text-[7.5vw] lg:text-[7rem]`}
          style={{
            color: C.cream,
            transform: `translate(${m.x * 10}px, ${m.y * 8}px)`,
            transition: "transform 500ms cubic-bezier(0.22,1,0.36,1)",
          }}
        >
          <SplitWords text="Practice problem solving one at a time" stagger={95} startDelay={200} />
        </h1>
      </div>

      {/* scroll cue */}
      <Reveal
        variant="fade"
        delay={1600}
        duration={900}
        className="absolute bottom-6 right-6 md:right-10 z-10 flex flex-col items-center gap-2"
      >
        <span className={`${mono} text-[9px] tracking-[0.3em]`} style={{ color: C.muted }}>
          SCROLL
        </span>
        <span
          className="w-px h-10 animate-float-y"
          style={{ background: `linear-gradient(${C.muted}, transparent)` }}
        />
      </Reveal>

      <div className="absolute bottom-6 left-6 md:left-10 z-10">
        <Reveal variant="up" delay={1400}>
          <Barcode color={C.muted} />
        </Reveal>
      </div>
    </section>
  );
};

/* -------------------------------------------------------------------------- */
/*  Split — the arena / the engine                                            */
/* -------------------------------------------------------------------------- */
const pins = [
  { label: "PROBLEM LIBRARY", note: "CURATED", color: C.orange },
  { label: "LIVE CODE JUDGE", note: "REAL-TIME", color: C.yellow },
  { label: "EDITORIALS", note: "DEEP DIVES", color: C.olive },
  { label: "SUBMISSION HISTORY", note: "TRACK GROWTH", color: C.rust },
];

const Split = () => (
  <section className="relative grid md:grid-cols-2" style={{ backgroundColor: C.bg }}>
    {/* left dark column */}
    <div className="px-6 md:px-10 py-20 md:py-28 flex flex-col justify-center">
      <Reveal variant="up">
        <p className={`${mono} text-[11px] tracking-[0.35em] mb-8`} style={{ color: C.orange }}>
          THE&nbsp;ARENA
        </p>
      </Reveal>
      <h2 className={`${serif} text-3xl md:text-5xl leading-tight max-w-[18ch]`} style={{ color: C.cream }}>
        <SplitWords
          text="Built for focus. Every problem, editorial, and submission in one calm workspace."
          stagger={55}
        />
      </h2>

      <ul className="mt-12 space-y-5">
        {pins.map((p, i) => (
          <Reveal as="li" key={p.label} variant="left" delay={200 + i * 120} className="block">
            <div className="flex items-center gap-4">
              <span className="relative inline-flex w-3 h-3 items-center justify-center">
                <span
                  className="absolute inset-0 rounded-full animate-ping-ring"
                  style={{ border: `1px solid ${p.color}`, animationDelay: `${i * 700}ms` }}
                />
                <span
                  className="relative inline-block w-3 h-3 rounded-full"
                  style={{ backgroundColor: p.color, boxShadow: `0 0 10px ${p.color}66` }}
                />
              </span>
              <span className={`${mono} text-xs tracking-[0.2em] flex-1`} style={{ color: C.cream }}>
                {p.label}
              </span>
              <span
                className={`${mono} text-[10px] tracking-[0.2em]`}
                style={{ color: p.color }}
              >
                {p.note}
              </span>
            </div>
          </Reveal>
        ))}
      </ul>
    </div>

    {/* right cream panel */}
    <div className="relative px-6 md:px-10 py-20 md:py-28 overflow-hidden" style={{ backgroundColor: C.cream }}>
      <Reveal variant="up">
        <p className={`${mono} text-[11px] tracking-[0.35em]`} style={{ color: C.rust }}>
          THE&nbsp;ENGINE
        </p>
      </Reveal>
      <CodeABadge />
      <Reveal variant="up" delay={1400}>
        <p className={`${mono} text-center text-xs tracking-[0.3em]`} style={{ color: C.ink }}>
          REAL-TIME&nbsp;·&nbsp;FAULT-TOLERANT
        </p>
      </Reveal>
    </div>
  </section>
);

/* -------------------------------------------------------------------------- */
/*  Statement                                                                  */
/* -------------------------------------------------------------------------- */
const Statement = () => (
  <section className="relative overflow-hidden py-28 md:py-40" style={{ backgroundColor: C.bg }}>
    <div className="absolute bottom-0 left-0 w-[45%] h-[40%] opacity-70">
      <Waveform bars={90} seed={23} rise={1.4} color={C.orange} flip breathe className="h-full" />
    </div>
    <div className="absolute bottom-0 right-0 w-[30%] h-[26%] opacity-50">
      <Waveform bars={60} seed={44} rise={1.8} color={C.olive} breathe className="h-full" />
    </div>

    <div className="relative z-10 mx-auto max-w-[1600px] px-6 md:px-10">
      <h2
        className={`${serif} uppercase text-center leading-[1.02] text-[9vw] md:text-[5.5vw] lg:text-[5rem] max-w-[16ch] mx-auto`}
        style={{ color: C.cream }}
      >
        <SplitWords text="Where algorithms meet craft" stagger={110} />
      </h2>

      <div className="mt-16 flex justify-center md:justify-end">
        <Reveal variant="scale" duration={1000} className="max-w-md p-8 md:p-10" style={{ backgroundColor: C.cream }}>
          <p className={`${mono} text-xs md:text-[13px] leading-relaxed tracking-wide`} style={{ color: C.ink }}>
            CODE ARENA IS A REAL-TIME JUDGE BUILT FOR SCALE — POWERED BY A
            DECOUPLED SUBMISSION AND EXECUTION PIPELINE ENGINEERED FOR FAULT
            TOLERANCE.
          </p>
          <p className="text-center mt-8 animate-shimmer-star" style={{ color: C.yellow }}>
            ✦ ✦ ✦
          </p>
        </Reveal>
      </div>
    </div>
  </section>
);

/* -------------------------------------------------------------------------- */
/*  Footer                                                                     */
/* -------------------------------------------------------------------------- */
const socials = [
  { Icon: Github, color: C.olive },
  { Icon: Twitter, color: C.orange },
  { Icon: Linkedin, color: C.yellow },
];

const Footer = () => (
  <footer className="relative overflow-hidden pt-20" style={{ backgroundColor: C.bg }}>
    {/* top nav row */}
    <Reveal
      variant="up"
      className="mx-auto max-w-[1600px] px-6 md:px-10 flex items-center justify-between mb-16"
    >
      <Link to="/explore" className={`${mono} text-[11px] tracking-[0.25em] hidden md:inline`} style={{ color: C.cream }}>
        PROBLEMS
      </Link>
      <span className={`${mono} font-bold text-xl tracking-[0.2em]`} style={{ color: C.cream }}>
        CODE ARENA
      </span>
      <Link
        to="/explore"
        className={`${mono} text-[11px] tracking-[0.2em] rounded-full px-6 py-3 transition-transform hover:scale-[1.03]`}
        style={{ backgroundColor: C.cream, color: C.ink }}
      >
        START SOLVING
      </Link>
    </Reveal>

    <div className="mx-auto max-w-[1600px] px-6 md:px-10 grid md:grid-cols-2 gap-12 pb-16">
      {/* left */}
      <Reveal variant="left">
        <p className={`${mono} text-[11px] tracking-[0.3em] mb-4`} style={{ color: C.olive }}>
          THE PLATFORM
        </p>
        <p className={`${serif} text-2xl leading-snug max-w-[22ch]`} style={{ color: C.cream }}>
          Train like the work matters. Practice with purpose.
        </p>
        <div className="flex gap-3 mt-8">
          {socials.map(({ Icon, color }, i) => (
            <Reveal key={i} variant="scale" delay={200 + i * 120}>
              <span
                className="w-10 h-10 rounded-full border flex items-center justify-center transition-all duration-300 hover:scale-110"
                style={{ borderColor: `${color}80` }}
              >
                <Icon className="w-4 h-4" style={{ color }} />
              </span>
            </Reveal>
          ))}
        </div>
      </Reveal>

      {/* right links */}
      <Reveal variant="right" className="md:text-right">
        <p className={`${mono} text-[11px] tracking-[0.3em] mb-4`} style={{ color: C.orange }}>
          EXPLORE
        </p>
        <ul className="space-y-3">
          {[
            { to: "/explore", label: "Problems" },
            { to: "/profile", label: "Profile" },
            { to: "/login", label: "Sign In" },
            { to: "/register", label: "Create Account" },
          ].map((l, i) => (
            <Reveal as="li" key={l.to} variant="up" delay={150 + i * 90}>
              <Link
                to={l.to}
                className={`${mono} text-sm tracking-wide inline-flex items-center gap-1 transition-all duration-300 hover:opacity-60 hover:gap-2`}
                style={{ color: C.cream }}
              >
                {l.label}
                <ArrowUpRight className="w-3.5 h-3.5" />
              </Link>
            </Reveal>
          ))}
        </ul>
      </Reveal>
    </div>

    {/* bottom bar over waveform */}
    <div className="relative h-24" style={{ backgroundColor: C.cream }}>
      <div className="absolute -top-16 left-0 right-0 h-16 overflow-hidden">
        <Waveform bars={220} seed={5} rise={1} color={C.cream} stagger={1400} className="h-full" />
      </div>
      <div className="relative mx-auto max-w-[1600px] px-6 md:px-10 h-full flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Barcode color={C.ink} />
          <span className={`${mono} text-[10px] md:text-[11px] tracking-[0.2em]`} style={{ color: C.ink }}>
            © 2026 CODE ARENA. ALL RIGHTS RESERVED
          </span>
        </div>
        <span className={`${mono} text-[10px] md:text-[11px] tracking-[0.2em]`} style={{ color: C.ink }}>
          PRIVACY POLICY
        </span>
      </div>
    </div>
  </footer>
);

/* -------------------------------------------------------------------------- */
const Home = () => (
  <div className="w-full" style={{ backgroundColor: C.bg }}>
    <Hero />
    <Split />
    <Statement />
    <Footer />
  </div>
);

export default Home;
