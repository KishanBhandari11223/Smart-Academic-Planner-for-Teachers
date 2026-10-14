export type PaceMode = "shared" | "independent";
export type SubjectStatus = "draft" | "planning" | "approved";
export type SessionStatus = "proposed" | "approved" | "completed" | "missed";
export type OutcomeKind = "completed" | "partial" | "not-covered";
export type Complexity = "Foundational" | "Moderate" | "Advanced";
export type Importance = "Core" | "Supporting" | "Enrichment";
export type View =
  | "home"
  | "subjects"
  | "subject"
  | "create"
  | "syllabus"
  | "pacing"
  | "schedule"
  | "review"
  | "calendar"
  | "update"
  | "adjustment"
  | "profile"
  | "notifications"
  | "settings";

export interface SyllabusTopic {
  id: string;
  title: string;
  description: string;
  unitId: string;
  complexity: Complexity;
  importance: Importance;
  estimatedHours: number;
}

export interface SyllabusUnit {
  id: string;
  title: string;
  description: string;
  topicIds: string[];
}

export interface TeachingSlot {
  dayOfWeek: number;
  startTime: string;
  durationMinutes: number;
  room: string;
}

export interface Subject {
  id: string;
  name: string;
  code: string;
  program: string;
  semester: string;
  academicYear: string;
  sections: string[];
  paceMode: PaceMode;
  units: SyllabusUnit[];
  topics: SyllabusTopic[];
  slots: TeachingSlot[];
  planStart: string;
  planEnd: string;
  status: SubjectStatus;
  objectives: string;
  createdAt: string;
}

export interface TeachingSession {
  id: string;
  subjectId: string;
  topicId: string;
  date: string;
  startTime: string;
  endTime: string;
  sectionIds: string[];
  status: SessionStatus;
  isManual?: boolean;
}

export interface SessionOutcome {
  id: string;
  sessionId: string;
  outcome: OutcomeKind;
  note: string;
  savedAt: string;
}

export interface ScheduleProposal {
  id: string;
  subjectId: string;
  sourceSessionId: string;
  outcome: OutcomeKind;
  reason: string;
  proposedSessions: TeachingSession[];
  affectedSessionIds: string[];
}

export interface AcademicCalendarEvent {
  id: string;
  date: string;
  title: string;
  kind: "holiday" | "exam";
}

export interface AppData {
  version: 1;
  subjects: Subject[];
  sessions: TeachingSession[];
  outcomes: SessionOutcome[];
  lastProposal: ScheduleProposal | null;
}
