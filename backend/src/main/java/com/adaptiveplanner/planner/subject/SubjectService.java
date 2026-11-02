package com.adaptiveplanner.planner.subject;

import com.adaptiveplanner.planner.account.TeacherAccount;
import com.adaptiveplanner.planner.account.TeacherAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SubjectService {
    private static final int NAME_LIMIT = 120;
    private static final int DESCRIPTION_LIMIT = 1000;

    private final SubjectRepository subjects;
    private final TeacherAccountRepository teachers;

    public SubjectService(SubjectRepository subjects, TeacherAccountRepository teachers) {
        this.subjects = subjects;
        this.teachers = teachers;
    }

    @Transactional(readOnly = true)
    public List<Subject> listForTeacher(String teacherId, boolean archived) {
        return subjects.findAllByTeacher_IdAndArchivedOrderByNameAsc(teacherId, archived);
    }

    @Transactional(readOnly = true)
    public Subject getForTeacher(String subjectId, String teacherId) {
        return subjects.findByIdAndTeacher_Id(subjectId, teacherId)
                .orElseThrow(() -> notFound("Subject"));
    }

    @Transactional
    public Subject create(String teacherId, String submittedName, String submittedDescription) {
        String name = requireName(submittedName);
        String description = normalizeDescription(submittedDescription);
        if (subjects.findByTeacher_IdAndNameIgnoreCase(teacherId, name).isPresent()) {
            throw new InvalidPlannerInputException("You already have a subject with that name.");
        }
        TeacherAccount teacher = teachers.findById(teacherId)
                .orElseThrow(() -> notFound("Teacher account"));
        return subjects.save(new Subject(teacher, name, description));
    }

    @Transactional
    public Subject update(String subjectId, String teacherId, String submittedName, String submittedDescription) {
        Subject subject = getForTeacher(subjectId, teacherId);
        String name = requireName(submittedName);
        String description = normalizeDescription(submittedDescription);
        subjects.findByTeacher_IdAndNameIgnoreCase(teacherId, name)
                .filter(existing -> !existing.getId().equals(subjectId))
                .ifPresent(existing -> { throw new InvalidPlannerInputException("You already have a subject with that name."); });
        subject.update(name, description);
        return subject;
    }

    @Transactional
    public void setArchived(String subjectId, String teacherId, boolean archived) {
        Subject subject = getForTeacher(subjectId, teacherId);
        subject.setArchived(archived);
    }

    private String requireName(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isBlank() || name.length() > NAME_LIMIT) {
            throw new InvalidPlannerInputException("Enter a subject name between 1 and 120 characters.");
        }
        return name;
    }

    private String normalizeDescription(String value) {
        String description = value == null ? "" : value.trim();
        if (description.length() > DESCRIPTION_LIMIT) {
            throw new InvalidPlannerInputException("Description must be 1,000 characters or fewer.");
        }
        return description.isEmpty() ? null : description;
    }

    private ResponseStatusException notFound(String type) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found.");
    }
}
