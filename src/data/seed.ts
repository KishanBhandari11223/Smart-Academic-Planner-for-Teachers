import { generateSchedule } from "@/lib/planning";
import type { AcademicCalendarEvent, AppData, Subject } from "@/types";

export const academicEvents: AcademicCalendarEvent[] = [
  { id: "holiday-diwali", date: "2026-10-21", title: "Institutional recess", kind: "holiday" },
  { id: "exam-window", date: "2026-11-09", title: "Mid-term assessment window", kind: "exam" },
  { id: "holiday-foundation", date: "2026-11-14", title: "University foundation day", kind: "holiday" },
];

const dataMining: Subject = {
  id: "subject-data-mining",
  name: "Data Mining & Warehousing",
  code: "COMP-5301",
  program: "MCA · Semester 3",
  semester: "Semester 3",
  academicYear: "2026–27",
  sections: ["MCA 3A", "MCA 3B"],
  paceMode: "shared",
  planStart: "2026-10-12",
  planEnd: "2026-12-18",
  status: "approved",
  objectives: "Build a strong foundation in data mining methods and warehouse architecture.",
  createdAt: "2026-08-01T10:00:00.000Z",
  units: [
    {
      id: "unit-foundations",
      title: "Foundations & System Architecture",
      description: "Core concepts, motivation, and system components.",
      topicIds: ["topic-foundations", "topic-primitives"],
    },
    {
      id: "unit-warehousing",
      title: "Data Warehousing & OLAP",
      description: "Warehouse architecture, OLAP, and data preparation.",
      topicIds: ["topic-warehouse", "topic-olap"],
    },
    {
      id: "unit-patterns",
      title: "Mining Frequent Patterns",
      description: "Association rules and practical pattern discovery.",
      topicIds: ["topic-association", "topic-apriori"],
    },
    {
      id: "unit-clustering",
      title: "Classification & Cluster Analysis",
      description: "Clustering methods and model evaluation.",
      topicIds: ["topic-clustering", "topic-evaluation"],
    },
  ],
  topics: [
    { id: "topic-foundations", unitId: "unit-foundations", title: "Data mining foundations & system architecture", description: "KDD process, motivation, and the major components of a mining system.", complexity: "Foundational", importance: "Core", estimatedHours: 1 },
    { id: "topic-primitives", unitId: "unit-foundations", title: "Data mining tasks, languages & query architectures", description: "Task primitives, query syntax, and data mining languages.", complexity: "Moderate", importance: "Core", estimatedHours: 2 },
    { id: "topic-warehouse", unitId: "unit-warehousing", title: "Warehouse modeling & integration", description: "Schemas, measures, concept hierarchies, and data integration.", complexity: "Moderate", importance: "Core", estimatedHours: 2 },
    { id: "topic-olap", unitId: "unit-warehousing", title: "OLAP operations and data cube computation", description: "OLAP models, cube computation, and analytical query patterns.", complexity: "Advanced", importance: "Core", estimatedHours: 2 },
    { id: "topic-association", unitId: "unit-patterns", title: "Association rules & support-confidence", description: "Frequent itemsets, support, confidence, and rule evaluation.", complexity: "Moderate", importance: "Core", estimatedHours: 2 },
    { id: "topic-apriori", unitId: "unit-patterns", title: "Apriori algorithm and pruning", description: "Candidate generation, pruning, and applied examples.", complexity: "Advanced", importance: "Core", estimatedHours: 2 },
    { id: "topic-clustering", unitId: "unit-clustering", title: "Clustering foundations & algorithms", description: "Partitioning, density-based, and hierarchical methods.", complexity: "Advanced", importance: "Core", estimatedHours: 2 },
    { id: "topic-evaluation", unitId: "unit-clustering", title: "Model evaluation and practical cases", description: "Validation, common pitfalls, and worked case studies.", complexity: "Moderate", importance: "Supporting", estimatedHours: 1 },
  ],
  slots: [
    { dayOfWeek: 1, startTime: "10:00", durationMinutes: 60, room: "Room 402" },
    { dayOfWeek: 3, startTime: "10:00", durationMinutes: 60, room: "Room 402" },
    { dayOfWeek: 5, startTime: "10:00", durationMinutes: 60, room: "Room 402" },
  ],
};

const seedSessions = generateSchedule(dataMining, new Set(academicEvents.map((event) => event.date))).map(
  (session, index) => ({ ...session, status: index < 2 ? "completed" as const : "approved" as const }),
);

export const initialData: AppData = {
  version: 1,
  subjects: [dataMining],
  sessions: seedSessions,
  outcomes: [
    {
      id: "outcome-seed-1",
      sessionId: seedSessions[0]?.id ?? "",
      outcome: "completed",
      note: "Students followed the KDD example confidently.",
      savedAt: "2026-10-12T11:10:00.000Z",
    },
  ],
  lastProposal: null,
};
