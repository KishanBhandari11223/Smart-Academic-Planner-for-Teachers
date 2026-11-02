ALTER TABLE planned_sessions
    ADD COLUMN session_type VARCHAR(16) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN source_session_id CHAR(36) NULL,
    ADD COLUMN class_outcome VARCHAR(16) NULL,
    ADD COLUMN covered_minutes INT NULL,
    ADD COLUMN outcome_notes VARCHAR(2000) NULL,
    ADD COLUMN outcome_logged_at TIMESTAMP(6) NULL,
    ADD CONSTRAINT fk_planned_sessions_source FOREIGN KEY (source_session_id) REFERENCES planned_sessions (id);

CREATE TABLE schedule_adjustment_proposals (
    id CHAR(36) NOT NULL PRIMARY KEY,
    plan_id CHAR(36) NOT NULL,
    source_session_id CHAR(36) NOT NULL,
    topic_id CHAR(36) NOT NULL,
    proposed_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    effort_minutes INT NOT NULL,
    topic_title_snapshot VARCHAR(180) NOT NULL,
    unit_name_snapshot VARCHAR(160) NOT NULL,
    important_snapshot BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    applied_session_id CHAR(36) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    decided_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_schedule_adjustments_plan FOREIGN KEY (plan_id) REFERENCES teaching_plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_schedule_adjustments_source FOREIGN KEY (source_session_id) REFERENCES planned_sessions (id),
    CONSTRAINT fk_schedule_adjustments_topic FOREIGN KEY (topic_id) REFERENCES syllabus_topics (id),
    CONSTRAINT fk_schedule_adjustments_applied_session FOREIGN KEY (applied_session_id) REFERENCES planned_sessions (id),
    INDEX idx_schedule_adjustments_plan_status (plan_id, status, created_at)
);
