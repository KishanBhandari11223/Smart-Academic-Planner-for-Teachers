CREATE TABLE syllabus_units (
    id CHAR(36) NOT NULL PRIMARY KEY,
    subject_id CHAR(36) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_syllabus_units_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
    INDEX idx_syllabus_units_subject_order (subject_id, sort_order)
);

CREATE TABLE syllabus_topics (
    id CHAR(36) NOT NULL PRIMARY KEY,
    unit_id CHAR(36) NOT NULL,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(2000) NULL,
    estimated_effort_minutes INT NOT NULL,
    important BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_syllabus_topics_unit FOREIGN KEY (unit_id) REFERENCES syllabus_units (id),
    INDEX idx_syllabus_topics_unit_order (unit_id, sort_order)
);
