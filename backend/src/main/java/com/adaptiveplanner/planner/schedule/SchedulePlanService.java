package com.adaptiveplanner.planner.schedule;

import com.adaptiveplanner.planner.subject.InvalidPlannerInputException;
import com.adaptiveplanner.planner.subject.TeachingSection;
import com.adaptiveplanner.planner.subject.TeachingSectionRepository;
import com.adaptiveplanner.planner.subject.WeeklyTeachingSlot;
import com.adaptiveplanner.planner.syllabus.SyllabusTopic;
import com.adaptiveplanner.planner.syllabus.SyllabusTopicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SchedulePlanService {
    private static final int NAME_LIMIT = 120;
    private static final int LABEL_LIMIT = 160;
    private static final long MAX_PLAN_DAYS = 366;

    private final TeachingSectionRepository sections;
    private final TeachingPlanRepository plans;
    private final TeachingPlanDayRepository planDays;
    private final TeachingPlanBlockedDateRepository blockedDates;
    private final PlannedSessionRepository sessions;
    private final UnplannedTopicRepository unplannedTopics;
    private final SyllabusTopicRepository topics;

    public SchedulePlanService(TeachingSectionRepository sections, TeachingPlanRepository plans,
                               TeachingPlanDayRepository planDays,
                               TeachingPlanBlockedDateRepository blockedDates,
                               PlannedSessionRepository sessions, UnplannedTopicRepository unplannedTopics,
                               SyllabusTopicRepository topics) {
        this.sections = sections;
        this.plans = plans;
        this.planDays = planDays;
        this.blockedDates = blockedDates;
        this.sessions = sessions;
        this.unplannedTopics = unplannedTopics;
        this.topics = topics;
    }

    @Transactional(readOnly = true)
    public TeachingSection getSectionForTeacher(String sectionId, String teacherId) {
        return ownedSection(sectionId, teacherId);
    }

    @Transactional(readOnly = true)
    public List<TeachingPlan> listPlans(String sectionId, String teacherId) {
        ownedSection(sectionId, teacherId);
        return plans.findAllForSection(sectionId, teacherId);
    }

    @Transactional
    public TeachingPlan createPlan(String sectionId, String teacherId, String submittedName,
                                   LocalDate startDate, LocalDate endDate, List<String> submittedDays) {
        TeachingSection section = editableSection(sectionId, teacherId);
        String name = normalizeName(submittedName);
        validateRange(startDate, endDate);
        Set<DayOfWeek> availableDays = availableDays(section);
        Set<DayOfWeek> selectedDays = parseDays(submittedDays);
        if (selectedDays.isEmpty()) {
            throw new InvalidPlannerInputException("Select at least one teaching day.");
        }
        if (!availableDays.containsAll(selectedDays)) {
            throw new InvalidPlannerInputException("Selected days must have a weekly teaching time for this section.");
        }

        TeachingPlan plan = plans.saveAndFlush(new TeachingPlan(section, name, startDate, endDate));
        List<TeachingPlanDay> dayRows = selectedDays.stream()
                .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                .map(day -> new TeachingPlanDay(plan, day))
                .toList();
        planDays.saveAll(dayRows);
        return plan;
    }

    @Transactional(readOnly = true)
    public PlanDetails getPlanDetails(String planId, String teacherId) {
        TeachingPlan plan = ownedPlan(planId, teacherId);
        return new PlanDetails(plan,
                planDays.findAllForPlan(planId, teacherId),
                blockedDates.findAllForPlan(planId, teacherId),
                sessions.findAllForPlan(planId, teacherId),
                unplannedTopics.findAllForPlan(planId, teacherId));
    }

    @Transactional
    public void addBlockedDate(String planId, String teacherId, LocalDate date,
                               String submittedLabel, String categoryValue) {
        TeachingPlan plan = requireDraft(planId, teacherId);
        if (date == null || date.isBefore(plan.getStartDate()) || date.isAfter(plan.getEndDate())) {
            throw new InvalidPlannerInputException("Choose a blocked date inside this plan's date range.");
        }
        String label = submittedLabel == null ? "" : submittedLabel.trim();
        if (label.isBlank() || label.length() > LABEL_LIMIT) {
            throw new InvalidPlannerInputException("Enter a label between 1 and 160 characters.");
        }
        BlockedDateCategory category;
        try {
            category = BlockedDateCategory.valueOf(categoryValue == null ? "" : categoryValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidPlannerInputException("Choose holiday, exam, or other for the blocked date.");
        }
        if (blockedDates.existsDate(planId, teacherId, date)) {
            throw new InvalidPlannerInputException("This date is already blocked for the plan.");
        }
        blockedDates.save(new TeachingPlanBlockedDate(plan, date, label, category));
        plan.markNeedsGeneration();
    }

    @Transactional
    public void removeBlockedDate(String planId, String blockedDateId, String teacherId) {
        TeachingPlan plan = requireDraft(planId, teacherId);
        TeachingPlanBlockedDate blockedDate = blockedDates.findOwned(blockedDateId, planId, teacherId)
                .orElseThrow(this::notFound);
        blockedDates.delete(blockedDate);
        plan.markNeedsGeneration();
    }

    @Transactional
    public GenerationSummary generateProposal(String planId, String teacherId) {
        TeachingPlan plan = requireDraft(planId, teacherId);
        List<TeachingPlanDay> selectedDayRows = planDays.findAllForPlan(planId, teacherId);
        Set<DayOfWeek> selectedDays = EnumSet.noneOf(DayOfWeek.class);
        selectedDayRows.forEach(row -> selectedDays.add(row.getDayOfWeek()));
        if (selectedDays.isEmpty()) throw new InvalidPlannerInputException("This plan has no selected teaching days.");

        List<TeachingPlanBlockedDate> blockedDateRows = blockedDates.findAllForPlan(planId, teacherId);
        Set<LocalDate> blocked = new HashSet<>();
        blockedDateRows.forEach(row -> blocked.add(row.getBlockedDate()));

        TeachingSection section = ownedSection(plan.getSection().getId(), teacherId);
        List<WeeklyTeachingSlot> weeklySlots = section.getWeeklySlots().stream()
                .filter(slot -> selectedDays.contains(slot.getDayOfWeek()))
                .sorted(Comparator.comparingInt((WeeklyTeachingSlot slot) -> slot.getDayOfWeek().getValue())
                        .thenComparing(WeeklyTeachingSlot::getStartTime))
                .toList();
        if (weeklySlots.isEmpty()) {
            throw new InvalidPlannerInputException("Add weekly teaching times for at least one selected day before generating.");
        }

        List<SyllabusTopic> orderedTopics = topics.findAllForSubjectAndTeacher(
                section.getSubject().getId(), teacherId);
        if (orderedTopics.isEmpty()) {
            throw new InvalidPlannerInputException("Add topics to this subject's syllabus before generating a plan.");
        }

        // Regeneration replaces only this draft plan's generated proposal rows.
        unplannedTopics.deleteAllForPlan(planId, teacherId);
        sessions.deleteAllForPlan(planId, teacherId);

        int topicIndex = 0;
        int remainingForTopic = orderedTopics.getFirst().getEstimatedEffortMinutes();
        int sessionOrder = 0;
        int scheduledTopicCount = 0;
        List<PlannedSession> proposedSessions = new ArrayList<>();

        for (LocalDate date = plan.getStartDate(); !date.isAfter(plan.getEndDate()) && topicIndex < orderedTopics.size(); date = date.plusDays(1)) {
            if (!selectedDays.contains(date.getDayOfWeek()) || blocked.contains(date)) continue;
            for (WeeklyTeachingSlot slot : weeklySlots) {
                if (slot.getDayOfWeek() != date.getDayOfWeek()) continue;
                LocalTime cursor = slot.getStartTime();
                while (cursor.isBefore(slot.getEndTime()) && topicIndex < orderedTopics.size()) {
                    long availableLong = Duration.between(cursor, slot.getEndTime()).toMinutes();
                    if (availableLong < 1) break;
                    int available = (int) Math.min(Integer.MAX_VALUE, availableLong);
                    int allocation = Math.min(available, remainingForTopic);
                    LocalTime sessionEnd = cursor.plusMinutes(allocation);
                    proposedSessions.add(new PlannedSession(plan, orderedTopics.get(topicIndex), date,
                            cursor, sessionEnd, allocation, ++sessionOrder));
                    cursor = sessionEnd;
                    remainingForTopic -= allocation;
                    if (remainingForTopic == 0) {
                        scheduledTopicCount++;
                        topicIndex++;
                        if (topicIndex < orderedTopics.size()) {
                            remainingForTopic = orderedTopics.get(topicIndex).getEstimatedEffortMinutes();
                        }
                    }
                }
            }
        }

        sessions.saveAll(proposedSessions);
        List<UnplannedTopic> remainingTopics = new ArrayList<>();
        int unscheduledEffort = 0;
        int unscheduledCount = 0;
        for (int i = topicIndex; i < orderedTopics.size(); i++) {
            SyllabusTopic topic = orderedTopics.get(i);
            int left = i == topicIndex ? remainingForTopic : topic.getEstimatedEffortMinutes();
            remainingTopics.add(new UnplannedTopic(plan, topic, i + 1, left));
            unscheduledEffort += left;
            unscheduledCount++;
        }
        unplannedTopics.saveAll(remainingTopics);
        plan.markGenerated(java.time.Instant.now());
        return new GenerationSummary(proposedSessions.size(), scheduledTopicCount, unscheduledCount, unscheduledEffort);
    }

    @Transactional
    public void approve(String planId, String teacherId) {
        TeachingPlan plan = requireDraft(planId, teacherId);
        if (sessions.countForPlan(planId, teacherId) == 0) {
            throw new InvalidPlannerInputException("Generate at least one scheduled time block before approving this plan.");
        }
        try {
            plan.approve(java.time.Instant.now());
        } catch (IllegalStateException exception) {
            throw new InvalidPlannerInputException(exception.getMessage());
        }
    }

    private TeachingPlan requireDraft(String planId, String teacherId) {
        TeachingPlan plan = ownedPlan(planId, teacherId);
        if (plan.getStatus() != SchedulePlanStatus.DRAFT) {
            throw new InvalidPlannerInputException("An approved plan can no longer be changed.");
        }
        return plan;
    }

    private TeachingPlan ownedPlan(String planId, String teacherId) {
        return plans.findOwned(planId, teacherId).orElseThrow(this::notFound);
    }

    private TeachingSection editableSection(String sectionId, String teacherId) {
        TeachingSection section = ownedSection(sectionId, teacherId);
        if (section.isArchived() || section.getSubject().isArchived()) {
            throw new InvalidPlannerInputException("Restore this subject and section before creating a plan.");
        }
        if (section.getWeeklySlots().isEmpty()) {
            throw new InvalidPlannerInputException("Add a weekly teaching time to this section before creating a plan.");
        }
        return section;
    }

    private TeachingSection ownedSection(String sectionId, String teacherId) {
        return sections.findOwnedWithSlots(sectionId, teacherId).orElseThrow(this::notFound);
    }

    private Set<DayOfWeek> availableDays(TeachingSection section) {
        Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
        section.getWeeklySlots().forEach(slot -> result.add(slot.getDayOfWeek()));
        return result;
    }

    private Set<DayOfWeek> parseDays(List<String> submittedDays) {
        Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
        if (submittedDays == null) return result;
        try {
            submittedDays.stream().map(value -> DayOfWeek.valueOf(value.trim().toUpperCase(Locale.ROOT)))
                    .forEach(result::add);
        } catch (RuntimeException exception) {
            throw new InvalidPlannerInputException("Choose valid weekdays for this plan.");
        }
        return result;
    }

    private String normalizeName(String submittedName) {
        String name = submittedName == null ? "" : submittedName.trim();
        if (name.isBlank() || name.length() > NAME_LIMIT) {
            throw new InvalidPlannerInputException("Enter a plan name between 1 and 120 characters.");
        }
        return name;
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new InvalidPlannerInputException("Choose a valid start and end date.");
        }
        if (Duration.between(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()).toDays() > MAX_PLAN_DAYS) {
            throw new InvalidPlannerInputException("A plan can cover no more than 366 days.");
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Teaching plan not found.");
    }
}
