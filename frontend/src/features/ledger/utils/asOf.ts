export function toAsOfParam(date: string | null): string | undefined {
  if (!date) return undefined;

  /* 
  DESIGN: I decided to use the user's local time instead of a specific 
  reporting timezone (e.g., always UTC or always the business' local time zone).
  I realize that this can lead to two different users getting different trial 
  balances if they are in different time zones. I may change this in the future.
  */

  const endOfDay = new Date(`${date}T23:59:59.999`);
  return endOfDay.toISOString(); // NOTE: Converts EOD browser's time to UTC
}
