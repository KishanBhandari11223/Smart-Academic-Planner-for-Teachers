package com.adaptiveplanner.planner.schedule;

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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "teaching_plan_blocked_dates", uniqueConstraints =
        @UniqueConstraint(name = "uk_teaching_plan_blocked_date", columnNames = {"plan_id", "blocked_date"}))
public class TeachingPlanBlockedDate {
    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private TeachingPlan plan;

    @Column(name = "blocked_date", nullable = false)
    private LocalDate blockedDate;

    @Column(length = 160, nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private BlockedDateCategory category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TeachingPlanBlockedDate() {
    }

    public TeachingPlanBlockedDate(TeachingPlan plan, LocalDate blockedDate, String label,
                                   BlockedDateCategory category) {
        this.plan = plan;
        this.blockedDate = blockedDate;
        this.label = label;
        this.category = category;
    }

    @PrePersist
    void beforeInsert() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public TeachingPlan getPlan() { return plan; }
    public LocalDate getBlockedDate() { return blockedDate; }
    public String getLabel() { return label; }
    public BlockedDateCategory getCategory() { return category; }
}
