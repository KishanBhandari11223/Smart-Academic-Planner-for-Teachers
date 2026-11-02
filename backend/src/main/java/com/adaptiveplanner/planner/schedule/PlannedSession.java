package com.adaptiveplanner.planner.schedule;

import com.adaptiveplanner.planner.syllabus.SyllabusTopic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "planned_sessions")
public class PlannedSession {
    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private TeachingPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private SyllabusTopic topic;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "effort_minutes", nullable = false)
    private int effortMinutes;

    @Column(name = "topic_title_snapshot", length = 180, nullable = false)
    private String topicTitleSnapshot;

    @Column(name = "unit_name_snapshot", length = 160, nullable = false)
    private String unitNameSnapshot;

    @Column(name = "important_snapshot", nullable = false)
    private boolean importantSnapshot;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", length = 16, nullable = false)
    private PlannedSessionType sessionType = PlannedSessionType.STANDARD;

    @Column(name = "source_session_id", length = 36)
    private String sourceSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "class_outcome", length = 16)
    private ClassOutcome classOutcome;

    @Column(name = "covered_minutes")
    private Integer coveredMinutes;

    @Column(name = "outcome_notes", length = 2000)
    private String outcomeNotes;

    @Column(name = "outcome_logged_at")
    private Instant outcomeLoggedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PlannedSession() {
    }

    public PlannedSession(TeachingPlan plan, SyllabusTopic topic, LocalDate scheduledDate,
                          LocalTime startTime, LocalTime endTime, int effortMinutes, int sortOrder) {
        this.plan = plan;
        this.topic = topic;
        this.scheduledDate = scheduledDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.effortMinutes = effortMinutes;
        this.topicTitleSnapshot = topic.getTitle();
        this.unitNameSnapshot = topic.getUnit().getName();
        this.importantSnapshot = topic.isImportant();
        this.sortOrder = sortOrder;
    }

    private PlannedSession(TeachingPlan plan, SyllabusTopic topic, LocalDate scheduledDate,
                           LocalTime startTime, LocalTime endTime, int effortMinutes, int sortOrder,
                           String sourceSessionId) {
        this(plan, topic, scheduledDate, startTime, endTime, effortMinutes, sortOrder);
        this.sessionType = PlannedSessionType.FOLLOW_UP;
        this.sourceSessionId = sourceSessionId;
    }

    public static PlannedSession followUp(TeachingPlan plan, SyllabusTopic topic, LocalDate scheduledDate,
                                          LocalTime startTime, LocalTime endTime, int effortMinutes,
                                          int sortOrder, String sourceSessionId) {
        return new PlannedSession(plan, topic, scheduledDate, startTime, endTime, effortMinutes,
                sortOrder, sourceSessionId);
    }

    public void recordOutcome(ClassOutcome outcome, int coveredMinutes, String notes, Instant loggedAt) {
        if (classOutcome != null) throw new IllegalStateException("An outcome has already been recorded.");
        this.classOutcome = outcome;
        this.coveredMinutes = coveredMinutes;
        this.outcomeNotes = notes;
        this.outcomeLoggedAt = loggedAt;
    }

    @PrePersist
    void beforeInsert() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getEffortMinutes() { return effortMinutes; }
    public String getTopicTitleSnapshot() { return topicTitleSnapshot; }
    public String getUnitNameSnapshot() { return unitNameSnapshot; }
    public boolean isImportantSnapshot() { return importantSnapshot; }
    public int getSortOrder() { return sortOrder; }
    public PlannedSessionType getSessionType() { return sessionType; }
    public String getSourceSessionId() { return sourceSessionId; }
    public ClassOutcome getClassOutcome() { return classOutcome; }
    public Integer getCoveredMinutes() { return coveredMinutes; }
    public String getOutcomeNotes() { return outcomeNotes; }
    public Instant getOutcomeLoggedAt() { return outcomeLoggedAt; }
    public TeachingPlan getPlan() { return plan; }
    public SyllabusTopic getTopic() { return topic; }
}
