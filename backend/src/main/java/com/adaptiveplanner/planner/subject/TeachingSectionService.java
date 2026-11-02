package com.adaptiveplanner.planner.subject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TeachingSectionService {
    private static final int NAME_LIMIT = 120;
    private static final int TERM_LIMIT = 120;
    private static final int LOCATION_LIMIT = 120;

    private final SubjectService subjects;
    private final TeachingSectionRepository sections;

    public TeachingSectionService(SubjectService subjects, TeachingSectionRepository sections) {
        this.subjects = subjects;
        this.sections = sections;
    }

    @Transactional(readOnly = true)
    public List<TeachingSection> listForSubject(String subjectId, String teacherId, boolean archived) {
        subjects.getForTeacher(subjectId, teacherId);
        return sections.findSectionsForTeacher(subjectId, teacherId, archived);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> activeSectionCounts(String teacherId, List<String> subjectIds) {
        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        return sections.countActiveSectionsForSubjects(teacherId, subjectIds).stream()
                .collect(Collectors.toMap(SubjectSectionCount::subjectId, SubjectSectionCount::sectionCount));
    }

    @Transactional(readOnly = true)
    public TeachingSection getForTeacher(String sectionId, String teacherId) {
        return sections.findOwnedWithSlots(sectionId, teacherId)
                .orElseThrow(() -> notFound());
    }

    @Transactional
    public TeachingSection create(String subjectId, String teacherId, String submittedName, String submittedTerm) {
        Subject subject = subjects.getForTeacher(subjectId, teacherId);
        if (subject.isArchived()) {
            throw new InvalidPlannerInputException("Restore this subject before adding a section.");
        }
        String name = requireName(submittedName);
        if (sections.existsName(subjectId, teacherId, name)) {
            throw new InvalidPlannerInputException("This subject already has a section with that name.");
        }
        return sections.save(new TeachingSection(subject, name, normalizeTerm(submittedTerm)));
    }

    @Transactional
    public TeachingSection update(String sectionId, String teacherId, String submittedName, String submittedTerm) {
        TeachingSection section = getForTeacher(sectionId, teacherId);
        String name = requireName(submittedName);
        String subjectId = section.getSubject().getId();
        if (sections.existsOtherName(subjectId, teacherId, name, sectionId)) {
            throw new InvalidPlannerInputException("This subject already has a section with that name.");
        }
        section.update(name, normalizeTerm(submittedTerm));
        return section;
    }

    @Transactional
    public String setArchived(String sectionId, String teacherId, boolean archived) {
        TeachingSection section = getForTeacher(sectionId, teacherId);
        section.setArchived(archived);
        return section.getSubject().getId();
    }

    @Transactional
    public void addWeeklySlot(String sectionId, String teacherId, String dayValue,
                              String startValue, String endValue, String submittedLocation) {
        TeachingSection section = getForTeacher(sectionId, teacherId);
        if (section.isArchived()) {
            throw new InvalidPlannerInputException("Restore this section before changing its weekly schedule.");
        }
        DayOfWeek day;
        LocalTime start;
        LocalTime end;
        try {
            day = DayOfWeek.valueOf(dayValue == null ? "" : dayValue.trim().toUpperCase(java.util.Locale.ROOT));
            start = LocalTime.parse(startValue == null ? "" : startValue.trim());
            end = LocalTime.parse(endValue == null ? "" : endValue.trim());
        } catch (RuntimeException exception) {
            throw new InvalidPlannerInputException("Choose a weekday and valid start and end times.");
        }
        String location = submittedLocation == null ? "" : submittedLocation.trim();
        if (location.length() > LOCATION_LIMIT) {
            throw new InvalidPlannerInputException("Location must be 120 characters or fewer.");
        }
        section.addWeeklySlot(day, start, end, location.isEmpty() ? null : location);
    }

    @Transactional
    public void removeWeeklySlot(String sectionId, String teacherId, String slotId) {
        TeachingSection section = getForTeacher(sectionId, teacherId);
        if (section.isArchived()) {
            throw new InvalidPlannerInputException("Restore this section before changing its weekly schedule.");
        }
        section.removeWeeklySlot(slotId);
    }

    private String requireName(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isBlank() || name.length() > NAME_LIMIT) {
            throw new InvalidPlannerInputException("Enter a section name between 1 and 120 characters.");
        }
        return name;
    }

    private String normalizeTerm(String value) {
        String term = value == null ? "" : value.trim();
        if (term.length() > TERM_LIMIT) {
            throw new InvalidPlannerInputException("Term must be 120 characters or fewer.");
        }
        return term.isEmpty() ? null : term;
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Teaching section not found.");
    }
}
