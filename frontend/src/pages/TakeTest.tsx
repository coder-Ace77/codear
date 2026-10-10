import { useEffect, useRef, useState } from "react";
import { Link, Navigate, useNavigate, useParams } from "react-router-dom";
import Button from "@/atoms/Button";
import { apiErrorText, testService, type ApiError } from "@/service/testService";

/** Landing page of a private invite link: /test/<token>. Starting is an explicit click, because a
 * timed test begins counting the moment it starts and a link preview or stray refresh must not start it. */
const TakeTest = () => {
  const { token = "" } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const signedIn = !!localStorage.getItem("token");
  const here = `/test/${token}`;

  const start = async () => {
    setBusy(true);
    setError(null);
    try {
      const attempt = await testService.startWithInvite(token);
      navigate(`/test/attempt/${attempt.attemptId}`, { replace: true });
    } catch (e) {
      setError(
        (e as ApiError)?.response?.status === 401
          ? "Your session has expired. Sign in again."
          : apiErrorText(e, "Could not open this test link."),
      );
      setBusy(false);
    }
  };

  return (
    <main className="mx-auto max-w-xl px-6 pb-16 pt-16">
      <h1 className="font-serif text-4xl font-medium tracking-[-0.02em]">You have been invited to a test</h1>
      {!signedIn ? (
        <>
          <p className="mt-3 text-muted-foreground">Sign in with the account this link was sent to.</p>
          <Link to={`/login?next=${encodeURIComponent(here)}`}>
            <Button className="mt-6">Sign in</Button>
          </Link>
        </>
      ) : (
        <>
          <p className="mt-3 text-muted-foreground">
            The link is personal and may only work once. If the test is timed, the clock starts when you press the button.
          </p>
          {error && <p className="mt-4 text-danger">{error}</p>}
          <Button className="mt-6" onClick={start} disabled={busy}>
            {busy ? "Starting…" : "Start test"}
          </Button>
        </>
      )}
    </main>
  );
};

export default TakeTest;
