CREATE TABLE teaching_plans (
    id CHAR(36) NOT NULL PRIMARY KEY,
    section_id CHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    generated_at TIMESTAMP(6) NULL,
    needs_generation BOOLEAN NOT NULL DEFAULT TRUE,
    approved_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_teaching_plans_section FOREIGN KEY (section_id) REFERENCES teaching_sections (id),
    INDEX idx_teaching_plans_section_status (section_id, status, start_date)
);

CREATE TABLE teaching_plan_days (
    id CHAR(36) NOT NULL PRIMARY KEY,
    plan_id CHAR(36) NOT NULL,
    day_of_week VARCHAR(9) NOT NULL,
    CONSTRAINT fk_teaching_plan_days_plan FOREIGN KEY (plan_id) REFERENCES teaching_plans (id) ON DELETE CASCADE,
    CONSTRAINT uk_teaching_plan_days_plan_day UNIQUE (plan_id, day_of_week)
);

CREATE TABLE teaching_plan_blocked_dates (
    id CHAR(36) NOT NULL PRIMARY KEY,
    plan_id CHAR(36) NOT NULL,
    blocked_date DATE NOT NULL,
    label VARCHAR(160) NOT NULL,
    category VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_teaching_plan_blocked_dates_plan FOREIGN KEY (plan_id) REFERENCES teaching_plans (id) ON DELETE CASCADE,
    CONSTRAINT uk_teaching_plan_blocked_date UNIQUE (plan_id, blocked_date),
    INDEX idx_teaching_plan_blocked_date_plan (plan_id, blocked_date)
);

CREATE TABLE planned_sessions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    plan_id CHAR(36) NOT NULL,
    topic_id CHAR(36) NOT NULL,
    scheduled_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    effort_minutes INT NOT NULL,
    topic_title_snapshot VARCHAR(180) NOT NULL,
    unit_name_snapshot VARCHAR(160) NOT NULL,
    important_snapshot BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_planned_sessions_plan FOREIGN KEY (plan_id) REFERENCES teaching_plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_planned_sessions_topic FOREIGN KEY (topic_id) REFERENCES syllabus_topics (id),
    INDEX idx_planned_sessions_plan_date_time (plan_id, scheduled_date, start_time)
);

CREATE TABLE unplanned_topics (
    id CHAR(36) NOT NULL PRIMARY KEY,
    plan_id CHAR(36) NOT NULL,
    topic_id CHAR(36) NOT NULL,
    topic_title_snapshot VARCHAR(180) NOT NULL,
    unit_name_snapshot VARCHAR(160) NOT NULL,
    important_snapshot BOOLEAN NOT NULL DEFAULT FALSE,
    topic_order INT NOT NULL,
    total_effort_minutes INT NOT NULL,
    remaining_effort_minutes INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_unplanned_topics_plan FOREIGN KEY (plan_id) REFERENCES teaching_plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_unplanned_topics_topic FOREIGN KEY (topic_id) REFERENCES syllabus_topics (id),
    INDEX idx_unplanned_topics_plan_order (plan_id, topic_order)
);
