package com.adaptiveplanner.planner.schedule;

public record GenerationSummary(int plannedSessions, int fullyScheduledTopics,
                                int topicsNotFullyScheduled, int remainingEffortMinutes) {
}
