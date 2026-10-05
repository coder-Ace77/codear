import { useCallback, useEffect, useRef, useState } from "react";
import Verdict from "@/atoms/Verdict";
import { adminService, AdminStats, RecentSubmission } from "@/service/adminService";

const REFRESH_MS = 3000;

const Tile = ({ label, value, note, warn }: { label: string; value: React.ReactNode; note?: string; warn?: boolean }) => (
  <div className="border-t border-border pt-2">
    <dt className="overline">{label}</dt>
    <dd className={`font-mono text-3xl font-medium ${warn ? "text-danger" : ""}`}>{value}</dd>
    {note && <p className="text-[13px] text-muted-foreground">{note}</p>}
  </div>
);

const age = (s: number | null) => {
  if (s === null) return "";
  if (s < 60) return `${s}s ago`;
  if (s < 3600) return `${Math.floor(s / 60)}m ago`;
  return `${Math.floor(s / 3600)}h ago`;
};

/** Live queue and submission activity. Polls the admin API every few seconds while the tab is visible. */
const AdminLive = () => {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [recent, setRecent] = useState<RecentSubmission[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [live, setLive] = useState(true);
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);
  const inFlight = useRef(false);

  const load = useCallback(async () => {
    if (inFlight.current) return; // never stack requests if the server is slow
    inFlight.current = true;
    try {
      const [s, r] = await Promise.all([adminService.stats(), adminService.recentSubmissions(30)]);
      setStats(s);
      setRecent(r);
      setError(null);
      setUpdatedAt(new Date());
    } catch (e: any) {
      setError(e.response?.status === 403 ? "You need an admin session to view this." : "Could not load live stats.");
    } finally {
      inFlight.current = false;
    }
  }, []);

  useEffect(() => {
    load();
    if (!live) return;
    const id = window.setInterval(() => {
      if (!document.hidden) load();
    }, REFRESH_MS);
    return () => window.clearInterval(id);
  }, [live, load]);

  const s = stats?.submissions;

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-baseline justify-between gap-3">
        <h2 className="font-serif text-2xl font-medium">Live activity</h2>
        <div className="flex items-center gap-3 text-[13px] text-muted-foreground">
          <span className="font-mono">
            {updatedAt ? `Updated ${updatedAt.toLocaleTimeString()}` : "Loading…"}
            {live ? ` · every ${REFRESH_MS / 1000}s` : " · paused"}
          </span>
          <button
            onClick={() => setLive((v) => !v)}
            className="inline-flex h-7 items-center rounded-sm border border-input px-3 font-sans text-[13px] font-semibold text-foreground transition-colors hover:border-foreground hover:bg-highlight-wash"
          >
            {live ? "Pause" : "Resume"}
          </button>
          <button
            onClick={load}
            className="inline-flex h-7 items-center rounded-sm border border-input px-3 font-sans text-[13px] font-semibold text-foreground transition-colors hover:border-foreground hover:bg-highlight-wash"
          >
            Refresh now
          </button>
        </div>
      </div>

      {error && <p className="mb-4 text-danger">{error}</p>}

      {stats && s && (
        <>
          <h3 className="overline mb-2 border-b border-foreground pb-2">Queues</h3>
          <dl className="mb-8 mt-4 grid grid-cols-2 gap-x-4 gap-y-6 md:grid-cols-4">
            {stats.queues.map((q) =>
              q.error || !q.configured ? (
                <Tile key={q.name} label={`${q.name} queue`} value="n/a" note={q.error ?? "Not configured"} warn={!!q.error} />
              ) : (
                <Tile
                  key={q.name}
                  label={`${q.name} queue`}
                  value={q.waiting}
                  note={`waiting · ${q.inFlight} being judged · ${q.delayed} delayed`}
                  warn={(q.waiting ?? 0) > 20}
                />
              )
            )}
            <Tile
              label="In progress"
              value={s.inProgress}
              note={
                s.oldestInProgressSeconds !== null
                  ? `oldest ${s.oldestInProgressSeconds}s${s.stuck ? " (possibly stuck)" : ""}`
                  : "nothing waiting"
              }
              warn={s.stuck}
            />
            <Tile
              label="Blocked by rate limit"
              value={stats.rateLimit.blockedLast5Minutes}
              note="in the last 5 minutes"
            />
          </dl>

          <h3 className="overline mb-2 border-b border-foreground pb-2">Submissions</h3>
          <dl className="mb-8 mt-4 grid grid-cols-2 gap-x-4 gap-y-6 md:grid-cols-4">
            <Tile label="Last minute" value={s.lastMinute} note={`${s.perSecondLastMinute}/s average`} />
            <Tile label="Last hour" value={s.lastHour} note={`${s.last5Minutes} in the last 5 minutes`} />
            <Tile label="Last 24 hours" value={s.last24Hours} note={`${s.total} all time`} />
            <Tile
              label="Average judge time"
              value={s.avgJudgeMsLastHour !== null ? `${s.avgJudgeMsLastHour} ms` : "n/a"}
              note="last hour"
            />
          </dl>

          <div className="mb-8 grid gap-8 md:grid-cols-2">
            <div>
              <h3 className="overline mb-2 border-b border-foreground pb-2">Outcomes, last 24 hours</h3>
              {Object.keys(s.byStatusLast24Hours).length === 0 ? (
                <p className="py-4 text-muted-foreground">No submissions in the last 24 hours.</p>
              ) : (
                <table className="w-full border-collapse">
                  <tbody>
                    {Object.entries(s.byStatusLast24Hours).map(([status, n]) => (
                      <tr key={status} className="border-b border-border">
                        <td className="py-2">
                          <Verdict status={status as any} />
                        </td>
                        <td className="py-2 text-right font-mono">{n}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            <div>
              <h3 className="overline mb-2 border-b border-foreground pb-2">Top submitters, last hour</h3>
              {stats.topSubmittersLastHour.length === 0 ? (
                <p className="py-4 text-muted-foreground">No submissions in the last hour.</p>
              ) : (
                <table className="w-full border-collapse">
                  <tbody>
                    {stats.topSubmittersLastHour.map((u) => (
                      <tr key={u.userId} className="border-b border-border">
                        <td className="py-2">{u.username}</td>
                        <td className="py-2 text-right font-mono">{u.submissions}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </>
      )}

      <h3 className="overline mb-0 border-b border-foreground pb-2">Latest submissions</h3>
      {recent.length === 0 ? (
        <p className="py-8 text-center text-muted-foreground">No submissions yet.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full border-collapse">
            <thead>
              <tr className="border-b border-border">
                {["When", "User", "Problem", "Language", "Result", "Tests", "Time"].map((h) => (
                  <th key={h} className="overline px-3 py-2 text-left">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {recent.map((r) => (
                <tr key={r.submissionId} className="border-b border-border hover:bg-highlight-wash">
                  <td className="whitespace-nowrap px-3 py-2 font-mono text-[13px] text-muted-foreground">{age(r.ageSeconds)}</td>
                  <td className="px-3 py-2">{r.username}</td>
                  <td className="px-3 py-2">
                    <span className="font-mono text-[13px] text-muted-foreground">{String(r.problemId).padStart(3, "0")}</span>{" "}
                    {r.problemTitle}
                  </td>
                  <td className="px-3 py-2 font-mono text-[13px]">{r.language}</td>
                  <td className="px-3 py-2">{r.status && <Verdict status={r.status === "IN_PROGRESS" ? "RUNNING" : (r.status as any)} />}</td>
                  <td className="px-3 py-2 font-mono text-[13px]">
                    {r.passedTests !== null && r.totalTests !== null ? `${r.passedTests}/${r.totalTests}` : ""}
                  </td>
                  <td className="px-3 py-2 font-mono text-[13px]">{r.timeTakenMs !== null ? `${r.timeTakenMs} ms` : ""}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default AdminLive;
