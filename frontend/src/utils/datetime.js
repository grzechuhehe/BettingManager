function pad2(n) {
  return String(n).padStart(2, '0');
}

// Formats a Date/parsable value as a zone-less `YYYY-MM-DDTHH:mm:ss` string
// using LOCAL clock components. Matches the backend's java.time.LocalDateTime
// semantics (no timezone). Returns null for empty/invalid input.
export function toLocalDateTimeString(value) {
  if (!value) return null;
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return (
    `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}` +
    `T${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`
  );
}
