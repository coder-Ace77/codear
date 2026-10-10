/**
 * The API sends UTC timestamps without a timezone suffix ("2026-10-06T15:52:10.4"). `new Date()` reads such a
 * string as local time, which is wrong by the user's UTC offset, so add the Z unless one is already there.
 */
export const parseUtc = (value: string) =>
  new Date(/[zZ]|[+-]\d\d:?\d\d$/.test(value) ? value : `${value}Z`);

/** A length of time for people: "45s", "12m 5s", "1h 20m". */
export const formatDuration = (seconds: number) => {
  if (seconds < 60) return `${seconds}s`;
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return h > 0 ? `${h}h ${m}m` : `${m}m ${seconds % 60}s`;
};
