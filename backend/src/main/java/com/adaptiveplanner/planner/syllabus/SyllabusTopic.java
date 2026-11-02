package com.adaptiveplanner.planner.syllabus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "syllabus_topics")
public class SyllabusTopic {
    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private SyllabusUnit unit;

    @Column(length = 180, nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "estimated_effort_minutes", nullable = false)
    private int estimatedEffortMinutes;

    @Column(nullable = false)
    private boolean important;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SyllabusTopic() {
    }

    public SyllabusTopic(SyllabusUnit unit, String title, String description, int effortMinutes,
                         boolean important, int sortOrder) {
        this.unit = unit;
        this.title = title;
        this.description = description;
        this.estimatedEffortMinutes = effortMinutes;
        this.important = important;
        this.sortOrder = sortOrder;
    }

    public void update(String title, String description, int effortMinutes, boolean important) {
        this.title = title;
        this.description = description;
        this.estimatedEffortMinutes = effortMinutes;
        this.important = important;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
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
    public SyllabusUnit getUnit() { return unit; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getEstimatedEffortMinutes() { return estimatedEffortMinutes; }
    public boolean isImportant() { return important; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
