
export const getLangOptions = (language) => {
  return {
    python: "python",
    cpp: "cpp",
  }[language];
}