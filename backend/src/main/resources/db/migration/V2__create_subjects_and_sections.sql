CREATE TABLE subjects (
    id CHAR(36) NOT NULL PRIMARY KEY,
    teacher_id CHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_subjects_teacher FOREIGN KEY (teacher_id) REFERENCES users (id),
    CONSTRAINT uk_subjects_teacher_name UNIQUE (teacher_id, name),
    INDEX idx_subjects_teacher_archived_name (teacher_id, archived, name)
);

CREATE TABLE teaching_sections (
    id CHAR(36) NOT NULL PRIMARY KEY,
    subject_id CHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    term_label VARCHAR(120) NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_teaching_sections_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT uk_teaching_sections_subject_name UNIQUE (subject_id, name),
    INDEX idx_teaching_sections_subject_archived (subject_id, archived, name)
);

CREATE TABLE weekly_teaching_slots (
    id CHAR(36) NOT NULL PRIMARY KEY,
    section_id CHAR(36) NOT NULL,
    day_of_week VARCHAR(9) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    location VARCHAR(120) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_weekly_slots_section FOREIGN KEY (section_id) REFERENCES teaching_sections (id) ON DELETE CASCADE,
    INDEX idx_weekly_slots_section_day_time (section_id, day_of_week, start_time)
);
