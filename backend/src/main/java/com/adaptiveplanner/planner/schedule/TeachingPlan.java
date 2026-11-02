package com.adaptiveplanner.planner.schedule;

import com.adaptiveplanner.planner.subject.TeachingSection;
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
import java.util.UUID;

@Entity
@Table(name = "teaching_plans")
public class TeachingPlan {
    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private TeachingSection section;

    @Column(length = 120, nullable = false)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private SchedulePlanStatus status = SchedulePlanStatus.DRAFT;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "needs_generation", nullable = false)
    private boolean needsGeneration = true;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected TeachingPlan() {
    }

    public TeachingPlan(TeachingSection section, String name, LocalDate startDate, LocalDate endDate) {
        this.section = section;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void markGenerated(Instant at) {
        this.generatedAt = at;
        this.needsGeneration = false;
    }

    public void markNeedsGeneration() {
        this.needsGeneration = true;
    }

    public void approve(Instant at) {
        if (status != SchedulePlanStatus.DRAFT || generatedAt == null || needsGeneration) {
            throw new IllegalStateException("Generate a current proposal before approving it.");
        }
        status = SchedulePlanStatus.APPROVED;
        approvedAt = at;
    }

    @PrePersist
    void beforeInsert() {
        if (id == null) id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public TeachingSection getSection() { return section; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public SchedulePlanStatus getStatus() { return status; }
    public Instant getGeneratedAt() { return generatedAt; }
    public boolean isNeedsGeneration() { return needsGeneration; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
