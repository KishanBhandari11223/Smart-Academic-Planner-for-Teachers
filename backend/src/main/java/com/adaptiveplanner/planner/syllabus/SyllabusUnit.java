package com.adaptiveplanner.planner.syllabus;

import com.adaptiveplanner.planner.subject.Subject;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "syllabus_units")
public class SyllabusUnit {
    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(length = 160, nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, title ASC")
    private List<SyllabusTopic> topics = new ArrayList<>();

    protected SyllabusUnit() {
    }

    public SyllabusUnit(Subject subject, String name, String description, int sortOrder) {
        this.subject = subject;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
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
    public Subject getSubject() { return subject; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<SyllabusTopic> getTopics() { return topics; }
}
