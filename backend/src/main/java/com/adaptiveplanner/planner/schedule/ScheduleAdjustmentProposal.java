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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "schedule_adjustment_proposals")
public class ScheduleAdjustmentProposal {
    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private TeachingPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_session_id", nullable = false)
    private PlannedSession sourceSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private SyllabusTopic topic;

    @Column(name = "proposed_date", nullable = false)
    private LocalDate proposedDate;

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

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private AdjustmentProposalStatus status = AdjustmentProposalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_session_id")
    private PlannedSession appliedSession;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Version
    private long version;

    protected ScheduleAdjustmentProposal() {
    }

    public ScheduleAdjustmentProposal(TeachingPlan plan, PlannedSession sourceSession, SyllabusTopic topic,
                                      LocalDate proposedDate, LocalTime startTime, LocalTime endTime,
                                      int effortMinutes) {
        this.plan = plan;
        this.sourceSession = sourceSession;
        this.topic = topic;
        this.proposedDate = proposedDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.effortMinutes = effortMinutes;
        this.topicTitleSnapshot = sourceSession.getTopicTitleSnapshot();
        this.unitNameSnapshot = sourceSession.getUnitNameSnapshot();
        this.importantSnapshot = sourceSession.isImportantSnapshot();
    }

    public void markApplied(PlannedSession appliedSession, Instant decidedAt) {
        requirePending();
        this.status = AdjustmentProposalStatus.APPLIED;
        this.appliedSession = appliedSession;
        this.decidedAt = decidedAt;
    }

    public void dismiss(Instant decidedAt) {
        requirePending();
        this.status = AdjustmentProposalStatus.DISMISSED;
        this.decidedAt = decidedAt;
    }

    private void requirePending() {
        if (status != AdjustmentProposalStatus.PENDING) {
            throw new IllegalStateException("This adjustment has already been decided.");
        }
    }

    @PrePersist
    void beforeInsert() {
        if (id == null) id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        createdAt = now;
        if (status == null) status = AdjustmentProposalStatus.PENDING;
    }

    @PreUpdate
    void beforeUpdate() {
        if (decidedAt == null && status != AdjustmentProposalStatus.PENDING) decidedAt = Instant.now();
    }

    public String getId() { return id; }
    public TeachingPlan getPlan() { return plan; }
    public PlannedSession getSourceSession() { return sourceSession; }
    public SyllabusTopic getTopic() { return topic; }
    public LocalDate getProposedDate() { return proposedDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getEffortMinutes() { return effortMinutes; }
    public String getTopicTitleSnapshot() { return topicTitleSnapshot; }
    public String getUnitNameSnapshot() { return unitNameSnapshot; }
    public boolean isImportantSnapshot() { return importantSnapshot; }
    public AdjustmentProposalStatus getStatus() { return status; }
    public PlannedSession getAppliedSession() { return appliedSession; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDecidedAt() { return decidedAt; }
}
