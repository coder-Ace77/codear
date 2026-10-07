const ExplorePageHeader = ({ grandTotalProblems, filtered }: { grandTotalProblems: number; filtered: boolean }) => {
  return (
    <div className="mb-6">
      <h1 className="font-serif text-5xl font-medium leading-[1.05] tracking-[-0.02em] md:text-[56px] md:leading-[60px]">
        Problems
      </h1>
      <p className="mt-1 text-muted-foreground">
        <span className="font-mono text-foreground">{grandTotalProblems}</span>{" "}
        {filtered ? (grandTotalProblems === 1 ? "problem matches." : "problems match.") : "problems to work through."}
      </p>
    </div>
  );
};

export default ExplorePageHeader;
