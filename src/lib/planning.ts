import type {
  AcademicCalendarEvent,
  ScheduleProposal,
  Subject,
  TeachingSession,
} from "@/types";

const pad = (value: number) => String(value).padStart(2, "0");

function isoDate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function addMinutes(time: string, minutes: number): string {
  const [hours = 0, mins = 0] = time.split(":").map(Number);
  const total = hours * 60 + mins + minutes;
  return `${pad(Math.floor(total / 60) % 24)}:${pad(total % 60)}`;
}

function localDate(dateString: string): Date {
  const [year, month, day] = dateString.split("-").map(Number);
  return new Date(year ?? 2026, (month ?? 1) - 1, day ?? 1, 12);
}

/**
 * A predictable slot allocator: topics stay in syllabus order and each estimated
 * teaching hour consumes one slot. Holidays and dates outside the selected period
 * are skipped; topics that exceed the available period remain visibly unscheduled.
 */
export function generateSchedule(
  subject: Subject,
  blockedDates: Set<string> = new Set(),
): TeachingSession[] {
  if (subject.slots.length === 0 || !subject.planStart || !subject.planEnd) return [];
  const first = localDate(subject.planStart);
  const last = localDate(subject.planEnd);
  if (first > last) return [];

  const slotsByDay = new Map<number, Subject["slots"]>();
  for (const slot of subject.slots) {
    const slots = slotsByDay.get(slot.dayOfWeek) ?? [];
    slots.push(slot);
    slotsByDay.set(slot.dayOfWeek, slots);
  }
  for (const slots of slotsByDay.values()) slots.sort((a, b) => a.startTime.localeCompare(b.startTime));

  const result: TeachingSession[] = [];
  const topics = subject.units.flatMap((unit) =>
    unit.topicIds
      .map((topicId) => subject.topics.find((topic) => topic.id === topicId))
      .filter((topic): topic is Subject["topics"][number] => Boolean(topic)),
  );
  let topicIndex = 0;
  let hourInTopic = 0;

  for (const date = new Date(first); date <= last && topicIndex < topics.length; date.setDate(date.getDate() + 1)) {
    const dateKey = isoDate(date);
    if (blockedDates.has(dateKey)) continue;
    const daySlots = slotsByDay.get(date.getDay()) ?? [];

    for (const slot of daySlots) {
      if (topicIndex >= topics.length) break;
      const topic = topics[topicIndex];
      if (!topic) break;

      const hoursNeeded = Math.max(1, Math.ceil(topic.estimatedHours));
      const sessionId = `${subject.id}-${topic.id}-${hourInTopic + 1}-${dateKey}`;
      result.push({
        id: sessionId,
        subjectId: subject.id,
        topicId: topic.id,
        date: dateKey,
        startTime: slot.startTime,
        endTime: addMinutes(slot.startTime, slot.durationMinutes),
        sectionIds: [...subject.sections],
        status: "proposed",
      });

      hourInTopic += 1;
      if (hourInTopic >= hoursNeeded) {
        topicIndex += 1;
        hourInTopic = 0;
      }
    }
  }

  return result;
}

/** Chooses the next open instructor slot and makes an explicit, reviewable proposal. */
export function buildAdjustmentProposal(
  subject: Subject,
  allSessions: TeachingSession[],
  sourceSession: TeachingSession,
  outcome: "partial" | "not-covered",
  blockedDates: Set<string>,
): ScheduleProposal | null {
  const topic = subject.topics.find((item) => item.id === sourceSession.topicId);
  if (!topic) return null;

  const occupied = new Set(
    allSessions
      .filter((session) => session.subjectId === subject.id && session.status === "approved")
      .map((session) => `${session.date}|${session.startTime}`),
  );
  const tomorrow = localDate(sourceSession.date);
  tomorrow.setDate(tomorrow.getDate() + 1);
  const end = localDate(subject.planEnd);
  let chosen: TeachingSession | null = null;

  for (const date = new Date(tomorrow); date <= end && !chosen; date.setDate(date.getDate() + 1)) {
    const dateKey = isoDate(date);
    if (blockedDates.has(dateKey)) continue;
    const slots = subject.slots
      .filter((slot) => slot.dayOfWeek === date.getDay())
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
    for (const slot of slots) {
      if (occupied.has(`${dateKey}|${slot.startTime}`)) continue;
      chosen = {
        id: `${subject.id}-adjust-${sourceSession.id}-${dateKey}`,
        subjectId: subject.id,
        topicId: topic.id,
        date: dateKey,
        startTime: slot.startTime,
        endTime: addMinutes(slot.startTime, slot.durationMinutes),
        sectionIds: [...sourceSession.sectionIds],
        status: "proposed",
      };
      break;
    }
  }

  if (!chosen) return null;
  return {
    id: `proposal-${sourceSession.id}`,
    subjectId: subject.id,
    sourceSessionId: sourceSession.id,
    outcome,
    reason:
      outcome === "partial"
        ? `Add a follow-up for “${topic.title}” in the next available teaching slot.`
        : `Move “${topic.title}” to the next available slot; the remaining plan stays unchanged.`,
    proposedSessions: [chosen],
    affectedSessionIds: [sourceSession.id],
  };
}

export function blockedDateSet(events: AcademicCalendarEvent[]): Set<string> {
  return new Set(events.map((event) => event.date));
}

export function formatDate(date: string, options?: Intl.DateTimeFormatOptions): string {
  return localDate(date).toLocaleDateString("en-IN", {
    weekday: "short",
    month: "short",
    day: "numeric",
    ...options,
  });
}
