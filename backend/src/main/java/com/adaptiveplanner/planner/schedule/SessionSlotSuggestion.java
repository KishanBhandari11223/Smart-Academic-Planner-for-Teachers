package com.adaptiveplanner.planner.schedule;

import java.time.LocalDate;
import java.time.LocalTime;

public record SessionSlotSuggestion(LocalDate date, LocalTime startTime, LocalTime endTime) {
}
