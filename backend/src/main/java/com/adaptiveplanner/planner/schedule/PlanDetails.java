package com.adaptiveplanner.planner.schedule;

import java.util.List;

public record PlanDetails(TeachingPlan plan, List<TeachingPlanDay> selectedDays,
                          List<TeachingPlanBlockedDate> blockedDates,
                          List<PlannedSession> sessions, List<UnplannedTopic> unplannedTopics) {
}
