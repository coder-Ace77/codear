import type { Monaco } from "@monaco-editor/react";

// Proof's editor palettes: paper-sunken ground, highlight-wash current line, syn-* token colours.
export const defineProofThemes = (monaco: Monaco) => {
  monaco.editor.defineTheme("proof-light", {
    base: "vs",
    inherit: true,
    rules: [
      { token: "keyword", foreground: "9a3412", fontStyle: "bold" },
      { token: "string", foreground: "0b6e63" },
      { token: "number", foreground: "1f4fbf" },
      { token: "comment", foreground: "5f5b52", fontStyle: "italic" },
    ],
    colors: {
      "editor.background": "#ece8dd",
      "editor.foreground": "#1c1b18",
      "editorLineNumber.foreground": "#5f5b52",
      "editorLineNumber.activeForeground": "#1c1b18",
      "editor.lineHighlightBackground": "#fbefc0",
      "editor.lineHighlightBorder": "#fbefc0",
      "editor.selectionBackground": "#ffd84a88",
      "editorCursor.foreground": "#1c1b18",
      "editorGutter.background": "#ece8dd",
    },
  });
  monaco.editor.defineTheme("proof-dark", {
    base: "vs-dark",
    inherit: true,
    rules: [
      { token: "keyword", foreground: "f0a35e", fontStyle: "bold" },
      { token: "string", foreground: "5fcdb8" },
      { token: "number", foreground: "8fb0ff" },
      { token: "comment", foreground: "a8a397", fontStyle: "italic" },
    ],
    colors: {
      "editor.background": "#100f0e",
      "editor.foreground": "#ede9df",
      "editorLineNumber.foreground": "#a8a397",
      "editorLineNumber.activeForeground": "#ede9df",
      "editor.lineHighlightBackground": "#3a3320",
      "editor.lineHighlightBorder": "#3a3320",
      "editor.selectionBackground": "#e8bf3a55",
      "editorCursor.foreground": "#ede9df",
      "editorGutter.background": "#100f0e",
    },
  });
};
