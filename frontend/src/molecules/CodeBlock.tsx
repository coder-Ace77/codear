import { useState } from "react";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { gruvboxDark } from "react-syntax-highlighter/dist/esm/styles/prism";
import { Copy, Check } from "lucide-react";
import { cn } from "@/lib/utils";

const langMap: Record<string, string> = {
  "c++": "cpp",
  cpp: "cpp",
  c: "c",
  java: "java",
  python: "python",
  python3: "python",
  py: "python",
  javascript: "javascript",
  js: "javascript",
  typescript: "typescript",
  ts: "typescript",
  go: "go",
  golang: "go",
  rust: "rust",
  kotlin: "kotlin",
  csharp: "csharp",
  "c#": "csharp",
  ruby: "ruby",
};

export const normalizeLang = (l?: string) =>
  (l && langMap[l.toLowerCase().trim()]) || "text";

/** Small copy-to-clipboard button with a copied confirmation state. */
export const CopyButton = ({ text, className }: { text: string; className?: string }) => {
  const [copied, setCopied] = useState(false);
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1600);
    } catch {
      /* clipboard unavailable */
    }
  };
  return (
    <button
      onClick={copy}
      className={cn(
        "flex items-center gap-1.5 font-['Space_Mono'] text-[10px] tracking-[0.15em] uppercase rounded-md px-2.5 py-1.5 transition-colors",
        copied
          ? "text-brand-olive"
          : "text-muted-foreground hover:text-brand-orange hover:bg-white/5",
        className
      )}
    >
      {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
      {copied ? "Copied" : "Copy"}
    </button>
  );
};

interface CodeBlockProps {
  code: string;
  language?: string;
  /** show the traffic-light dots in the header */
  dots?: boolean;
  /** render without an outer frame/header — just highlighted code + a floating copy button */
  bare?: boolean;
  className?: string;
}

const highlighterStyle = {
  margin: 0,
  background: "transparent",
  padding: "1rem 1.25rem",
  fontSize: "0.8rem",
  lineHeight: 1.6,
} as const;

/** Modern code block: header bar (language + copy) over gruvbox syntax highlighting. */
const CodeBlock = ({ code, language, dots = true, bare = false, className }: CodeBlockProps) => {
  const lang = normalizeLang(language);

  if (bare) {
    return (
      <div className={cn("relative group", className)} style={{ backgroundColor: "#282828" }}>
        <div className="absolute top-2 right-2 z-10 opacity-70 group-hover:opacity-100 transition-opacity">
          <CopyButton text={code} />
        </div>
        <SyntaxHighlighter
          language={lang}
          style={gruvboxDark}
          wrapLongLines
          customStyle={{ ...highlighterStyle, paddingRight: "4.5rem" }}
          codeTagProps={{ style: { fontFamily: "'JetBrains Mono', monospace" } }}
        >
          {code}
        </SyntaxHighlighter>
      </div>
    );
  }

  return (
    <div
      className={cn("rounded-lg overflow-hidden border border-border", className)}
      style={{ backgroundColor: "#282828" }}
    >
      <div className="flex items-center justify-between px-3 py-2 border-b border-white/10 bg-black/25">
        <div className="flex items-center gap-2">
          {dots && (
            <div className="flex items-center gap-1.5 mr-1">
              <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: "hsl(var(--brand-rust))" }} />
              <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: "hsl(var(--brand-yellow))" }} />
              <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: "hsl(var(--brand-olive))" }} />
            </div>
          )}
          <span className="font-['Space_Mono'] text-[10px] tracking-[0.2em] uppercase text-muted-foreground">
            {language || "code"}
          </span>
        </div>
        <CopyButton text={code} />
      </div>
      <SyntaxHighlighter
        language={lang}
        style={gruvboxDark}
        wrapLongLines
        customStyle={highlighterStyle}
        codeTagProps={{ style: { fontFamily: "'JetBrains Mono', monospace" } }}
      >
        {code}
      </SyntaxHighlighter>
    </div>
  );
};

export default CodeBlock;
