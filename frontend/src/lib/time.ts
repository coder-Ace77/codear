/**
 * The API sends UTC timestamps without a timezone suffix ("2026-10-06T15:52:10.4"). `new Date()` reads such a
 * string as local time, which is wrong by the user's UTC offset, so add the Z unless one is already there.
 */
export const parseUtc = (value: string) =>
  new Date(/[zZ]|[+-]\d\d:?\d\d$/.test(value) ? value : `${value}Z`);
