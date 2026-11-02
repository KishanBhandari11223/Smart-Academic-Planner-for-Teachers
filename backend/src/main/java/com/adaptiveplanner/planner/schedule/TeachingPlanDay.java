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

import java.time.DayOfWeek;
import java.util.UUID;

@Entity
@Table(name = "teaching_plan_days", uniqueConstraints =
        @UniqueConstraint(name = "uk_teaching_plan_days_plan_day", columnNames = {"plan_id", "day_of_week"}))
public class TeachingPlanDay {
    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private TeachingPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 9, nullable = false)
    private DayOfWeek dayOfWeek;

    protected TeachingPlanDay() {
    }

    public TeachingPlanDay(TeachingPlan plan, DayOfWeek dayOfWeek) {
        this.plan = plan;
        this.dayOfWeek = dayOfWeek;
    }

    @PrePersist
    void beforeInsert() {
        if (id == null) id = UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public TeachingPlan getPlan() { return plan; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
}
