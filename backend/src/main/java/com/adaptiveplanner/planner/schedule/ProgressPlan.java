package com.adaptiveplanner.planner.schedule;

import java.time.LocalDate;
import java.util.List;

public record ProgressPlan(String id, String name, String subjectName, String sectionName,
                           LocalDate startDate, LocalDate endDate,
                           int plannedMinutes, int coveredMinutes, int percentage,
                           int loggedSessions, int sessionCount, List<ProgressTopic> topics) {
}
