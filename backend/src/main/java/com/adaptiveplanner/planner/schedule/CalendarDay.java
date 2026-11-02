package com.adaptiveplanner.planner.schedule;

import java.time.LocalDate;
import java.util.List;

public record CalendarDay(LocalDate date, List<PlannedSession> sessions) {
}
