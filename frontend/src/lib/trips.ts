/**
 * Formats an ISO "YYYY-MM-DD" start/end pair into a compact range, e.g. "Oct 10 - Oct 12, 2026".
 * Parses y/m/d manually (not `new Date(str)`) to avoid a UTC-vs-local off-by-one-day shift.
 */
export function formatDateRange(startDate: string, endDate: string): string {
  const start = parseIsoDate(startDate)
  const end = parseIsoDate(endDate)

  const monthDay = new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
  })
  const monthDayYear = new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  })

  return `${monthDay.format(start)} - ${monthDayYear.format(end)}`
}

function parseIsoDate(isoDate: string): Date {
  const [year, month, day] = isoDate.split("-").map(Number)
  return new Date(year, month - 1, day)
}

/** Formats "HH:mm:ss" times into a "09:00 - 11:00" display range. */
export function formatTimeRange(startTime: string, endTime: string): string {
  return `${startTime.slice(0, 5)} - ${endTime.slice(0, 5)}`
}
