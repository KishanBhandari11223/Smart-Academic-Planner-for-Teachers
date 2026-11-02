package com.adaptiveplanner.planner.schedule;

import java.util.List;

public record ProgressDashboard(int plannedMinutes, int coveredMinutes, int percentage,
                                int loggedSessions, int sessionCount, List<ProgressPlan> plans) {
}
