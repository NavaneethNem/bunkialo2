import type { AcademicEvent } from "@/types";

export const getAcademicEventsForWindow = (
  events: AcademicEvent[],
  startDate: string,
  endDate: string,
): AcademicEvent[] =>
  events
    .filter((event) => {
      const eventEndDate = event.endDate ?? event.date;
      return event.date <= endDate && eventEndDate >= startDate;
    })
    .sort((first, second) => first.date.localeCompare(second.date));
