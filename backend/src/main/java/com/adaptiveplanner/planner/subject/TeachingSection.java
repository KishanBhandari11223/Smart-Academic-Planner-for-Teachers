package com.adaptiveplanner.planner.subject;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "teaching_sections", uniqueConstraints =
        @UniqueConstraint(name = "uk_teaching_sections_subject_name", columnNames = {"subject_id", "name"}))
public class TeachingSection {
    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(length = 120, nullable = false)
    private String name;

    @Column(name = "term_label", length = 120)
    private String termLabel;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeeklyTeachingSlot> weeklySlots = new ArrayList<>();

    protected TeachingSection() {
    }

    public TeachingSection(Subject subject, String name, String termLabel) {
        this.subject = subject;
        this.name = name;
        this.termLabel = termLabel;
    }

    public void update(String name, String termLabel) {
        this.name = name;
        this.termLabel = termLabel;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void addWeeklySlot(DayOfWeek day, LocalTime start, LocalTime end, String location) {
        if (!end.isAfter(start)) {
            throw new InvalidPlannerInputException("End time must be later than start time.");
        }
        boolean overlaps = weeklySlots.stream().anyMatch(slot -> slot.getDayOfWeek() == day
                && start.isBefore(slot.getEndTime()) && end.isAfter(slot.getStartTime()));
        if (overlaps) {
            throw new InvalidPlannerInputException("This time overlaps another weekly slot for the section.");
        }
        weeklySlots.add(new WeeklyTeachingSlot(this, day, start, end, location));
    }

    public void removeWeeklySlot(String slotId) {
        boolean removed = weeklySlots.removeIf(slot -> slot.getId().equals(slotId));
        if (!removed) {
            throw new InvalidPlannerInputException("That weekly slot could not be found.");
        }
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
    public Subject getSubject() { return subject; }
    public String getName() { return name; }
    public String getTermLabel() { return termLabel; }
    public boolean isArchived() { return archived; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<WeeklyTeachingSlot> getWeeklySlots() { return weeklySlots; }
}
