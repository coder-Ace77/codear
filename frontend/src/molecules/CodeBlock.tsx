import { useState } from "react";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
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

// Proof's syntax colours, read from the theme tokens so light and dark both work.
const proofPrism = {
  'code[class*="language-"]': { color: "hsl(var(--ink))", background: "none" },
  'pre[class*="language-"]': { color: "hsl(var(--ink))", background: "none" },
  comment: { color: "hsl(var(--ink-muted))", fontStyle: "italic" },
  prolog: { color: "hsl(var(--ink-muted))" },
  punctuation: { color: "hsl(var(--ink))" },
  keyword: { color: "hsl(var(--syn-keyword))", fontWeight: 500 },
  "control-flow": { color: "hsl(var(--syn-keyword))", fontWeight: 500 },
  builtin: { color: "hsl(var(--syn-keyword))" },
  string: { color: "hsl(var(--success))" },
  char: { color: "hsl(var(--success))" },
  number: { color: "hsl(var(--info))" },
  boolean: { color: "hsl(var(--info))" },
  constant: { color: "hsl(var(--info))" },
  function: { color: "hsl(var(--ink))" },
  operator: { color: "hsl(var(--ink))" },
} as const;

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
        "flex h-6 items-center gap-1.5 rounded-sm border border-input bg-card px-2 font-sans text-xs font-medium text-foreground transition-colors hover:bg-highlight-wash",
        className
      )}
    >
      {copied ? <Check className="h-3 w-3" aria-hidden="true" /> : <Copy className="h-3 w-3" aria-hidden="true" />}
      {copied ? "Copied" : "Copy"}
    </button>
  );
};

interface CodeBlockProps {
  code: string;
  language?: string;
  /** kept for existing callers; Proof has no window dots */
  dots?: boolean;
  /** render without an outer frame or header: just highlighted code and a floating copy button */
  bare?: boolean;
  className?: string;
}

const highlighterStyle = {
  margin: 0,
  background: "transparent",
  padding: "0.75rem 1rem",
  fontSize: "13px",
  lineHeight: "22px",
} as const;

const codeTagProps = { style: { fontFamily: '"IBM Plex Mono", ui-monospace, Menlo, monospace' } };

/** Code on paper-sunken, highlighted with the Proof syntax tokens. */
const CodeBlock = ({ code, language, bare = false, className }: CodeBlockProps) => {
  const lang = normalizeLang(language);

  if (bare) {
    return (
      <div className={cn("relative bg-secondary", className)}>
        <div className="absolute right-2 top-2 z-10">
          <CopyButton text={code} />
        </div>
        <SyntaxHighlighter
          language={lang}
          style={proofPrism as any}
          wrapLongLines
          customStyle={{ ...highlighterStyle, paddingRight: "5rem" }}
          codeTagProps={codeTagProps}
        >
          {code}
        </SyntaxHighlighter>
      </div>
    );
  }

  return (
    <div className={cn("overflow-hidden rounded-md border border-border bg-secondary", className)}>
      <div className="flex h-10 items-center justify-between border-b border-border bg-card px-3">
        <span className="font-mono text-[13px] font-medium">{language || "code"}</span>
        <CopyButton text={code} />
      </div>
      <SyntaxHighlighter
        language={lang}
        style={proofPrism as any}
        wrapLongLines
        customStyle={highlighterStyle}
        codeTagProps={codeTagProps}
      >
        {code}
      </SyntaxHighlighter>
    </div>
  );
};

export default CodeBlock;
