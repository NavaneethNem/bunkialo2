import {
  addDays,
  formatLongDate,
  parseISODate,
  toISODate,
} from "@/components/acad-cal/constants";
import { ACADEMIC_EVENTS } from "@/data/acad-cal";
import { useAcademicCalendarStore } from "@/stores/academic-calendar-store";
import type { AcademicEvent, AcademicEventOverride } from "@/types";
import { getAcademicEventsForWindow } from "@/utils/academic-calendar-window";
import { router } from "expo-router";
import { useMemo } from "react";
import { Text, View } from "react-native";
import { AcademicEventCard } from "./academic-event-card";

const WINDOW_DAYS = 7;

const mergeBaseEvent = (
  event: AcademicEvent,
  override: AcademicEventOverride | undefined,
): AcademicEvent | null => {
  if (override?.hidden) return null;

  const { hidden: _hidden, ...eventOverride } = override ?? {};
  return { ...event, ...eventOverride };
};

const groupEventsByDate = (
  events: AcademicEvent[],
  windowStart: string,
): Array<[string, AcademicEvent[]]> => {
  const groups = new Map<string, AcademicEvent[]>();

  events.forEach((event) => {
    const displayDate = event.date < windowStart ? windowStart : event.date;
    const existing = groups.get(displayDate) ?? [];
    groups.set(displayDate, [...existing, event]);
  });

  return Array.from(groups.entries());
};

export const AcademicEventsSection = () => {
  const { overrides, customEvents } = useAcademicCalendarStore();
  const windowStart = toISODate(new Date());
  const windowEnd = toISODate(
    addDays(parseISODate(windowStart), WINDOW_DAYS - 1),
  );

  const events = useMemo(() => {
    const baseEvents = ACADEMIC_EVENTS.flatMap((event) => {
      const merged = mergeBaseEvent(event, overrides[event.id]);
      return merged ? [merged] : [];
    });

    return getAcademicEventsForWindow(
      [...baseEvents, ...customEvents],
      windowStart,
      windowEnd,
    );
  }, [customEvents, overrides, windowEnd, windowStart]);

  const groupedEvents = useMemo(
    () => groupEventsByDate(events, windowStart),
    [events, windowStart],
  );

  if (groupedEvents.length === 0) return null;

  return (
    <View className="mt-6 gap-4">
      <View>
        <Text className="text-lg font-bold tracking-tight" style={{ color: "#111827" }}>
          Academic Calendar
        </Text>
        <Text className="mt-1 text-xs" style={{ color: "#6B7280" }}>
          Events in the next 7 days
        </Text>
      </View>

      {groupedEvents.map(([date, dateEvents]) => (
        <View key={date} className="gap-3">
          <Text className="text-sm font-semibold" style={{ color: "#6B7280" }}>
            {formatLongDate(date)}
          </Text>
          <View className="gap-3">
            {dateEvents.map((event) => (
              <AcademicEventCard
                key={event.id}
                event={event}
                onPress={() => router.push("/acad-cal")}
              />
            ))}
          </View>
        </View>
      ))}
    </View>
  );
};
