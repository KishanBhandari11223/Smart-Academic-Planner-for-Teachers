package com.adaptiveplanner.planner.schedule;

import com.adaptiveplanner.planner.syllabus.SyllabusTopic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "unplanned_topics")
public class UnplannedTopic {
    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private TeachingPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private SyllabusTopic topic;

    @Column(name = "topic_title_snapshot", length = 180, nullable = false)
    private String topicTitleSnapshot;

    @Column(name = "unit_name_snapshot", length = 160, nullable = false)
    private String unitNameSnapshot;

    @Column(name = "important_snapshot", nullable = false)
    private boolean importantSnapshot;

    @Column(name = "topic_order", nullable = false)
    private int topicOrder;

    @Column(name = "total_effort_minutes", nullable = false)
    private int totalEffortMinutes;

    @Column(name = "remaining_effort_minutes", nullable = false)
    private int remainingEffortMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UnplannedTopic() {
    }

    public UnplannedTopic(TeachingPlan plan, SyllabusTopic topic, int topicOrder,
                          int remainingEffortMinutes) {
        this.plan = plan;
        this.topic = topic;
        this.topicTitleSnapshot = topic.getTitle();
        this.unitNameSnapshot = topic.getUnit().getName();
        this.importantSnapshot = topic.isImportant();
        this.topicOrder = topicOrder;
        this.totalEffortMinutes = topic.getEstimatedEffortMinutes();
        this.remainingEffortMinutes = remainingEffortMinutes;
    }

    @PrePersist
    void beforeInsert() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getTopicTitleSnapshot() { return topicTitleSnapshot; }
    public String getUnitNameSnapshot() { return unitNameSnapshot; }
    public boolean isImportantSnapshot() { return importantSnapshot; }
    public int getTopicOrder() { return topicOrder; }
    public int getTotalEffortMinutes() { return totalEffortMinutes; }
    public int getRemainingEffortMinutes() { return remainingEffortMinutes; }
}
