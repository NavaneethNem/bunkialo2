import { describe, expect, test } from "bun:test";
import { parseGoogleCalendarFeed } from "@/services/academic-calendar-feed";

const SAMPLE_FEED = `BEGIN:VCALENDAR
VERSION:2.0
X-WR-TIMEZONE:Asia/Kolkata
BEGIN:VEVENT
UID:orientation-123@google.com
DTSTART:20260922T110000Z
DTEND:20260922T130000Z
SUMMARY:Enigma Orientation
LOCATION:BC 302\\,303
DESCRIPTION:Club orientation
STATUS:CONFIRMED
END:VEVENT
BEGIN:VEVENT
UID:festival-456@google.com
DTSTART;VALUE=DATE:20261009
DTEND;VALUE=DATE:20261012
SUMMARY:AAROH '26
STATUS:CONFIRMED
END:VEVENT
END:VCALENDAR`;

describe("Google Calendar feed", () => {
  test("normalizes timed events with local time, venue, and direct link", () => {
    const [event] = parseGoogleCalendarFeed(
      SAMPLE_FEED,
      new Date("2026-09-20T00:00:00.000Z"),
    );

    expect(event).toMatchObject({
      allDay: false,
      category: "club",
      date: "2026-09-22",
      endAt: "2026-09-22T13:00:00.000Z",
      location: "BC 302,303",
      origin: "google-calendar",
      startAt: "2026-09-22T11:00:00.000Z",
      title: "Enigma Orientation",
    });
    expect(event?.calendarUrl).toContain("calendar.google.com/calendar/event?eid=");
  });

  test("converts exclusive all-day end dates into an inclusive range", () => {
    const events = parseGoogleCalendarFeed(
      SAMPLE_FEED,
      new Date("2026-10-01T00:00:00.000Z"),
    );

    expect(events).toHaveLength(1);
    expect(events[0]).toMatchObject({
      allDay: true,
      date: "2026-10-09",
      endDate: "2026-10-11",
      title: "AAROH '26",
    });
    expect(events[0]?.startAt).toBeUndefined();
  });
});
