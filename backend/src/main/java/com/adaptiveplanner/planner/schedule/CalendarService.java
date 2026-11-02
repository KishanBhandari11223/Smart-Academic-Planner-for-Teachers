package com.adaptiveplanner.planner.schedule;

import com.adaptiveplanner.planner.subject.TeachingSection;
import com.adaptiveplanner.planner.subject.TeachingSectionRepository;
import com.adaptiveplanner.planner.subject.WeeklyTeachingSlot;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CalendarService {
    private static final long MAX_CALENDAR_DAYS = 366;
    private static final int NOTES_LIMIT = 2000;
    private static final int FOLLOW_UP_SEARCH_DAYS = 366;

    private final PlannedSessionRepository sessions;
    private final TeachingPlanRepository plans;
    private final TeachingPlanDayRepository planDays;
    private final TeachingPlanBlockedDateRepository blockedDates;
    private final TeachingSectionRepository sections;
    private final ScheduleAdjustmentProposalRepository adjustments;

    public CalendarService(PlannedSessionRepository sessions, TeachingPlanRepository plans,
                           TeachingPlanDayRepository planDays,
                           TeachingPlanBlockedDateRepository blockedDates,
                           TeachingSectionRepository sections,
                           ScheduleAdjustmentProposalRepository adjustments) {
        this.sessions = sessions;
        this.plans = plans;
        this.planDays = planDays;
        this.blockedDates = blockedDates;
        this.sections = sections;
        this.adjustments = adjustments;
    }

    @Transactional(readOnly = true)
    public CalendarView getCalendar(String teacherId, LocalDate fromDate, LocalDate toDate) {
        validateRange(fromDate, toDate);
        List<PlannedSession> rows = sessions.findCalendarSessions(teacherId, SchedulePlanStatus.APPROVED,
                fromDate, toDate);
        Map<LocalDate, List<PlannedSession>> grouped = new LinkedHashMap<>();
        rows.forEach(session -> grouped.computeIfAbsent(session.getScheduledDate(), ignored -> new ArrayList<>())
                .add(session));
        List<CalendarDay> days = grouped.entrySet().stream()
                .map(entry -> new CalendarDay(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
        return new CalendarView(fromDate, toDate, days,
                adjustments.findAllForTeacherAndStatus(teacherId, AdjustmentProposalStatus.PENDING));
    }

    @Transactional(readOnly = true)
    public PlannedSession getSessionForTeacher(String sessionId, String teacherId) {
        PlannedSession session = sessions.findOwnedForOutcome(sessionId, teacherId).orElseThrow(this::notFound);
        if (session.getPlan().getStatus() != SchedulePlanStatus.APPROVED) throw notFound();
        return session;
    }

    @Transactional
    public ClassUpdateResult recordOutcome(String sessionId, String teacherId, ClassOutcome outcome,
                                           Integer submittedCoveredMinutes, String submittedNotes) {
        PlannedSession session = sessions.findOwnedForOutcome(sessionId, teacherId).orElseThrow(this::notFound);
        if (session.getPlan().getStatus() != SchedulePlanStatus.APPROVED) throw notFound();
        if (session.getClassOutcome() != null) {
            throw new InvalidCalendarActionException("A class outcome has already been recorded for this session.");
        }
        if (session.getScheduledDate().isAfter(LocalDate.now())) {
            throw new InvalidCalendarActionException("Class outcomes can be recorded on or after the scheduled date.");
        }
        if (outcome == null) throw new InvalidCalendarActionException("Choose the class outcome.");

        int coveredMinutes = switch (outcome) {
            case COMPLETED -> session.getEffortMinutes();
            case NOT_COVERED -> 0;
            case PARTIAL -> {
                if (submittedCoveredMinutes == null || submittedCoveredMinutes < 1
                        || submittedCoveredMinutes >= session.getEffortMinutes()) {
                    throw new InvalidCalendarActionException("For a partial update, enter minutes covered between 1 and the planned effort.");
                }
                yield submittedCoveredMinutes;
            }
        };
        String notes = submittedNotes == null ? "" : submittedNotes.trim();
        if (notes.length() > NOTES_LIMIT) {
            throw new InvalidCalendarActionException("Class notes must be 2,000 characters or fewer.");
        }
        session.recordOutcome(outcome, coveredMinutes, notes.isEmpty() ? null : notes, Instant.now());
        int remainingMinutes = session.getEffortMinutes() - coveredMinutes;
        if (remainingMinutes == 0) {
            return new ClassUpdateResult(outcome, null, null, "Class update saved as completed.");
        }

        TeachingPlan plan = session.getPlan();
        String sectionId = plan.getSection().getId();
        TeachingSection section = sections.findOwnedWithSlots(sectionId, teacherId).orElseThrow(this::notFound);
        Set<DayOfWeek> selectedDays = EnumSet.noneOf(DayOfWeek.class);
        planDays.findAllForPlan(plan.getId(), teacherId).forEach(day -> selectedDays.add(day.getDayOfWeek()));
        Set<LocalDate> blocked = new HashSet<>();
        blockedDates.findAllForSection(sectionId, teacherId).forEach(date -> blocked.add(date.getBlockedDate()));
        List<PlannedSession> existing = sessions.findAllApprovedForSection(sectionId, teacherId,
                SchedulePlanStatus.APPROVED);
        SessionSlotSuggestion suggestion = findNextSlot(session, section, selectedDays, blocked,
                existing, remainingMinutes);
        if (suggestion == null) {
            return new ClassUpdateResult(outcome, null, null,
                    "Class update saved, but no free selected weekly slot can fit the remaining effort in the next year.");
        }

        ScheduleAdjustmentProposal proposal = adjustments.save(new ScheduleAdjustmentProposal(plan, session,
                session.getTopic(), suggestion.date(), suggestion.startTime(), suggestion.endTime(), remainingMinutes));
        return new ClassUpdateResult(outcome, proposal.getId(), suggestion.date(),
                "Class update saved. A follow-up time is ready for review.");
    }

    @Transactional
    public void applyAdjustment(String proposalId, String teacherId) {
        ScheduleAdjustmentProposal proposal = pendingProposal(proposalId, teacherId);
        TeachingPlan plan = plans.findOwned(proposal.getPlan().getId(), teacherId).orElseThrow(this::notFound);
        if (plan.getStatus() != SchedulePlanStatus.APPROVED) {
            throw new InvalidCalendarActionException("Only an approved plan can receive a follow-up session.");
        }
        String sectionId = plan.getSection().getId();
        TeachingSection section = sections.findOwnedWithSlots(sectionId, teacherId).orElseThrow(this::notFound);
        Set<DayOfWeek> selectedDays = EnumSet.noneOf(DayOfWeek.class);
        planDays.findAllForPlan(plan.getId(), teacherId).forEach(day -> selectedDays.add(day.getDayOfWeek()));
        Set<LocalDate> blocked = new HashSet<>();
        blockedDates.findAllForSection(sectionId, teacherId).forEach(date -> blocked.add(date.getBlockedDate()));
        List<PlannedSession> existing = sessions.findAllApprovedForSection(sectionId, teacherId,
                SchedulePlanStatus.APPROVED);
        if (!fitsAvailableSlot(proposal.getProposedDate(), proposal.getStartTime(), proposal.getEndTime(),
                proposal.getEffortMinutes(), section, selectedDays, blocked, existing)) {
            throw new InvalidCalendarActionException("That time is no longer free. Keep the original schedule and record a new class update if needed.");
        }
        int order = Math.toIntExact(sessions.countForPlan(plan.getId(), teacherId) + 1);
        PlannedSession followUp = PlannedSession.followUp(plan, proposal.getTopic(), proposal.getProposedDate(),
                proposal.getStartTime(), proposal.getEndTime(), proposal.getEffortMinutes(), order,
                proposal.getSourceSession().getId());
        sessions.save(followUp);
        proposal.markApplied(followUp, Instant.now());
    }

    @Transactional
    public void dismissAdjustment(String proposalId, String teacherId) {
        ScheduleAdjustmentProposal proposal = pendingProposal(proposalId, teacherId);
        proposal.dismiss(Instant.now());
    }

    private ScheduleAdjustmentProposal pendingProposal(String proposalId, String teacherId) {
        ScheduleAdjustmentProposal proposal = adjustments.findOwned(proposalId, teacherId).orElseThrow(this::notFound);
        if (proposal.getStatus() != AdjustmentProposalStatus.PENDING) {
            throw new InvalidCalendarActionException("This follow-up proposal has already been decided.");
        }
        return proposal;
    }

    private SessionSlotSuggestion findNextSlot(PlannedSession source, TeachingSection section,
                                               Set<DayOfWeek> selectedDays, Set<LocalDate> blocked,
                                               List<PlannedSession> existing, int effortMinutes) {
        if (selectedDays.isEmpty()) return null;
        List<WeeklyTeachingSlot> weeklySlots = section.getWeeklySlots().stream()
                .filter(slot -> selectedDays.contains(slot.getDayOfWeek()))
                .sorted(Comparator.comparingInt((WeeklyTeachingSlot slot) -> slot.getDayOfWeek().getValue())
                        .thenComparing(WeeklyTeachingSlot::getStartTime))
                .toList();
        LocalDate firstDate = source.getScheduledDate();
        LocalDate finalDate = firstDate.plusDays(FOLLOW_UP_SEARCH_DAYS);
        for (LocalDate date = firstDate; !date.isAfter(finalDate); date = date.plusDays(1)) {
            if (!selectedDays.contains(date.getDayOfWeek()) || blocked.contains(date)) continue;
            for (WeeklyTeachingSlot slot : weeklySlots) {
                if (slot.getDayOfWeek() != date.getDayOfWeek()) continue;
                LocalTime earliest = slot.getStartTime();
                if (date.equals(firstDate) && source.getEndTime().isAfter(earliest)) earliest = source.getEndTime();
                SessionSlotSuggestion available = findGap(date, slot, earliest, effortMinutes, existing);
                if (available != null) return available;
            }
        }
        return null;
    }

    private boolean fitsAvailableSlot(LocalDate date, LocalTime start, LocalTime end, int effortMinutes,
                                      TeachingSection section, Set<DayOfWeek> selectedDays,
                                      Set<LocalDate> blocked, List<PlannedSession> existing) {
        if (!selectedDays.contains(date.getDayOfWeek()) || blocked.contains(date)) return false;
        if (Duration.between(start, end).toMinutes() != effortMinutes) return false;
        for (WeeklyTeachingSlot slot : section.getWeeklySlots()) {
            if (slot.getDayOfWeek() != date.getDayOfWeek() || start.isBefore(slot.getStartTime())
                    || end.isAfter(slot.getEndTime())) continue;
            boolean conflict = existing.stream().anyMatch(session -> session.getScheduledDate().equals(date)
                    && start.isBefore(session.getEndTime()) && end.isAfter(session.getStartTime()));
            if (!conflict) return true;
        }
        return false;
    }

    private SessionSlotSuggestion findGap(LocalDate date, WeeklyTeachingSlot slot, LocalTime earliest,
                                          int effortMinutes, List<PlannedSession> existing) {
        LocalTime cursor = earliest;
        List<PlannedSession> conflicts = existing.stream()
                .filter(session -> session.getScheduledDate().equals(date)
                        && session.getStartTime().isBefore(slot.getEndTime())
                        && session.getEndTime().isAfter(slot.getStartTime()))
                .sorted(Comparator.comparing(PlannedSession::getStartTime))
                .toList();
        for (PlannedSession conflict : conflicts) {
            if (!conflict.getEndTime().isAfter(cursor)) continue;
            if (conflict.getStartTime().isAfter(cursor)
                    && Duration.between(cursor, conflict.getStartTime()).toMinutes() >= effortMinutes) {
                return new SessionSlotSuggestion(date, cursor, cursor.plusMinutes(effortMinutes));
            }
            if (conflict.getStartTime().isBefore(cursor) || conflict.getStartTime().equals(cursor)) {
                if (conflict.getEndTime().isAfter(cursor)) cursor = conflict.getEndTime();
            } else if (conflict.getStartTime().isAfter(cursor)) {
                cursor = conflict.getEndTime();
            }
            if (!cursor.isBefore(slot.getEndTime())) return null;
        }
        if (Duration.between(cursor, slot.getEndTime()).toMinutes() >= effortMinutes) {
            return new SessionSlotSuggestion(date, cursor, cursor.plusMinutes(effortMinutes));
        }
        return null;
    }

    private void validateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || toDate.isBefore(fromDate)
                || Duration.between(fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay()).toDays() > MAX_CALENDAR_DAYS) {
            throw new InvalidCalendarActionException("Choose a calendar range of 1 to 366 days.");
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Calendar item not found.");
    }
}
