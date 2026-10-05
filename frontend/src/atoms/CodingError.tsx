const CodingError = ({ error }) => {
  return (
    <div className="flex h-[calc(100vh-3.5rem)] flex-col items-center justify-center text-center">
      <h2 className="font-serif text-2xl font-medium text-danger">Could not load the problem</h2>
      <p className="mt-2 text-muted-foreground">{error}</p>
    </div>
  );
}

export default CodingError;
