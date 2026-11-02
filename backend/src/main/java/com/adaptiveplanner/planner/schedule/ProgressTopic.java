package com.adaptiveplanner.planner.schedule;

public record ProgressTopic(String unitName, String title, int plannedMinutes, int coveredMinutes,
                            int percentage, String status) {
}
