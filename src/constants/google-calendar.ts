export const GOOGLE_CALENDAR_ID =
  "c_6924fc2085f9177ac3c9719e1db5188a11bb7461cae26d03757a1db670a649ae@group.calendar.google.com";

export const GOOGLE_CALENDAR_TIME_ZONE = "Asia/Kolkata";

export const GOOGLE_CALENDAR_ICS_URL =
  `https://calendar.google.com/calendar/ical/${encodeURIComponent(GOOGLE_CALENDAR_ID)}/public/basic.ics`;

export const GOOGLE_CALENDAR_EMBED_URL =
  `https://calendar.google.com/calendar/embed?src=${encodeURIComponent(GOOGLE_CALENDAR_ID)}&ctz=${encodeURIComponent(GOOGLE_CALENDAR_TIME_ZONE)}`;
