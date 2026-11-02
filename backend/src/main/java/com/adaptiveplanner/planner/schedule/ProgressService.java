package com.adaptiveplanner.planner.schedule;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProgressService {
    private final PlannedSessionRepository sessions;

    public ProgressService(PlannedSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public ProgressDashboard getDashboard(String teacherId) {
        Map<String, PlanAccumulator> plans = new LinkedHashMap<>();
        for (PlannedSession session : sessions.findProgressSessions(teacherId, SchedulePlanStatus.APPROVED)) {
            var plan = session.getPlan();
            PlanAccumulator accumulator = plans.computeIfAbsent(plan.getId(), ignored -> new PlanAccumulator(
                    plan.getId(), plan.getName(), plan.getSection().getSubject().getName(), plan.getSection().getName(),
                    plan.getStartDate(), plan.getEndDate()));
            accumulator.add(session);
        }

        List<ProgressPlan> planSummaries = plans.values().stream().map(PlanAccumulator::toSummary).toList();
        int planned = planSummaries.stream().mapToInt(ProgressPlan::plannedMinutes).sum();
        int covered = planSummaries.stream().mapToInt(ProgressPlan::coveredMinutes).sum();
        int logged = planSummaries.stream().mapToInt(ProgressPlan::loggedSessions).sum();
        int count = planSummaries.stream().mapToInt(ProgressPlan::sessionCount).sum();
        return new ProgressDashboard(planned, covered, percentage(covered, planned), logged, count, planSummaries);
    }

    private static int percentage(int covered, int planned) {
        if (planned <= 0) return 0;
        return Math.min(100, (int) Math.round(covered * 100.0 / planned));
    }

    private static final class PlanAccumulator {
        private final String name;
        private final String id;
        private final String subjectName;
        private final String sectionName;
        private final java.time.LocalDate startDate;
        private final java.time.LocalDate endDate;
        private final Map<String, TopicAccumulator> topics = new LinkedHashMap<>();
        private int loggedSessions;
        private int sessionCount;

        private PlanAccumulator(String id, String name, String subjectName, String sectionName,
                                java.time.LocalDate startDate, java.time.LocalDate endDate) {
            this.id = id;
            this.name = name;
            this.subjectName = subjectName;
            this.sectionName = sectionName;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        private void add(PlannedSession session) {
            sessionCount++;
            if (session.getClassOutcome() != null) loggedSessions++;
            String topicId = session.getTopic().getId();
            TopicAccumulator topic = topics.computeIfAbsent(topicId, ignored -> new TopicAccumulator(
                    session.getUnitNameSnapshot(), session.getTopicTitleSnapshot()));
            topic.add(session);
        }

        private ProgressPlan toSummary() {
            List<ProgressTopic> topicSummaries = topics.values().stream().map(TopicAccumulator::toSummary).toList();
            int planned = topicSummaries.stream().mapToInt(ProgressTopic::plannedMinutes).sum();
            int covered = topicSummaries.stream().mapToInt(ProgressTopic::coveredMinutes).sum();
            return new ProgressPlan(id, name, subjectName, sectionName, startDate, endDate, planned, covered,
                    percentage(covered, planned), loggedSessions, sessionCount, topicSummaries);
        }
    }

    private static final class TopicAccumulator {
        private final String unitName;
        private final String title;
        private int standardPlannedMinutes;
        private int allPlannedMinutes;
        private int coveredMinutes;

        private TopicAccumulator(String unitName, String title) {
            this.unitName = unitName;
            this.title = title;
        }

        private void add(PlannedSession session) {
            allPlannedMinutes += session.getEffortMinutes();
            if (session.getSessionType() == PlannedSessionType.STANDARD) {
                standardPlannedMinutes += session.getEffortMinutes();
            }
            if (session.getCoveredMinutes() != null) coveredMinutes += session.getCoveredMinutes();
        }

        private ProgressTopic toSummary() {
            int planned = standardPlannedMinutes > 0 ? standardPlannedMinutes : allPlannedMinutes;
            int covered = Math.min(coveredMinutes, planned);
            String status = covered >= planned ? "Complete" : covered > 0 ? "In progress" : "Not started";
            return new ProgressTopic(unitName, title, planned, covered, percentage(covered, planned), status);
        }
    }
}
