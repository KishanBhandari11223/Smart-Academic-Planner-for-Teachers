package com.adaptiveplanner.planner.schedule;

import java.time.LocalDate;
import java.util.List;

public record CalendarView(LocalDate fromDate, LocalDate toDate,
                           List<CalendarDay> days, List<ScheduleAdjustmentProposal> pendingAdjustments) {
}
