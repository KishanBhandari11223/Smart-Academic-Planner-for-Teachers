package com.adaptiveplanner.planner.syllabus;

import com.adaptiveplanner.planner.subject.InvalidPlannerInputException;
import com.adaptiveplanner.planner.subject.Subject;
import com.adaptiveplanner.planner.subject.SubjectService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SyllabusService {
    private static final int UNIT_NAME_LIMIT = 160;
    private static final int TOPIC_TITLE_LIMIT = 180;
    private static final int DESCRIPTION_LIMIT = 2000;
    private static final int MAX_EFFORT_MINUTES = 10080;

    private final SubjectService subjects;
    private final SyllabusUnitRepository units;
    private final SyllabusTopicRepository topics;

    public SyllabusService(SubjectService subjects, SyllabusUnitRepository units,
                          SyllabusTopicRepository topics) {
        this.subjects = subjects;
        this.units = units;
        this.topics = topics;
    }

    @Transactional(readOnly = true)
    public Subject getSubject(String subjectId, String teacherId) {
        return subjects.getForTeacher(subjectId, teacherId);
    }

    @Transactional(readOnly = true)
    public List<SyllabusUnit> listUnits(String subjectId, String teacherId) {
        subjects.getForTeacher(subjectId, teacherId);
        return units.findAllForTeacher(subjectId, teacherId);
    }

    @Transactional(readOnly = true)
    public SyllabusUnit getUnit(String unitId, String teacherId) {
        return units.findOwned(unitId, teacherId).orElseThrow(this::notFound);
    }

    @Transactional(readOnly = true)
    public SyllabusUnit getUnitForSubject(String unitId, String subjectId, String teacherId) {
        SyllabusUnit unit = getUnit(unitId, teacherId);
        if (!unit.getSubject().getId().equals(subjectId)) throw notFound();
        return unit;
    }

    @Transactional
    public SyllabusUnit createUnit(String subjectId, String teacherId, String submittedName,
                                  String submittedDescription) {
        Subject subject = requireEditableSubject(subjectId, teacherId);
        String name = requireText(submittedName, UNIT_NAME_LIMIT, "unit name", 160);
        String description = normalizeDescription(submittedDescription);
        if (units.existsName(subjectId, teacherId, name)) {
            throw new InvalidPlannerInputException("This subject already has a unit with that name.");
        }
        return units.save(new SyllabusUnit(subject, name, description,
                units.findMaxOrder(subjectId, teacherId) + 1));
    }

    @Transactional
    public SyllabusUnit updateUnit(String subjectId, String unitId, String teacherId,
                                   String submittedName, String submittedDescription) {
        requireEditableSubject(subjectId, teacherId);
        SyllabusUnit unit = getUnitForSubject(unitId, subjectId, teacherId);
        String name = requireText(submittedName, UNIT_NAME_LIMIT, "unit name", 160);
        String description = normalizeDescription(submittedDescription);
        if (units.existsOtherName(subjectId, teacherId, name, unitId)) {
            throw new InvalidPlannerInputException("This subject already has a unit with that name.");
        }
        unit.update(name, description);
        return unit;
    }

    @Transactional
    public boolean moveUnit(String subjectId, String unitId, String teacherId, String direction) {
        requireEditableSubject(subjectId, teacherId);
        List<SyllabusUnit> ordered = units.findAllForTeacher(subjectId, teacherId);
        int index = indexOfUnit(ordered, unitId);
        int destination = index + directionOffset(direction);
        if (index < 0 || destination < 0 || destination >= ordered.size()) return false;
        SyllabusUnit current = ordered.get(index);
        SyllabusUnit neighbor = ordered.get(destination);
        int currentOrder = current.getSortOrder();
        current.setSortOrder(neighbor.getSortOrder());
        neighbor.setSortOrder(currentOrder);
        return true;
    }

    @Transactional(readOnly = true)
    public SyllabusTopic getTopic(String subjectId, String unitId, String topicId, String teacherId) {
        getUnitForSubject(unitId, subjectId, teacherId);
        return topics.findForUnitAndTeacher(topicId, unitId, teacherId).orElseThrow(this::notFound);
    }

    @Transactional
    public SyllabusTopic createTopic(String subjectId, String unitId, String teacherId, String submittedTitle,
                                     String submittedDescription, int effortMinutes, boolean important) {
        Subject subject = requireEditableSubject(subjectId, teacherId);
        SyllabusUnit unit = getUnitForSubject(unitId, subjectId, teacherId);
        String title = requireText(submittedTitle, TOPIC_TITLE_LIMIT, "topic title", 180);
        String description = normalizeDescription(submittedDescription);
        validateEffort(effortMinutes);
        if (subject.isArchived()) throw new InvalidPlannerInputException("Restore this subject before editing its syllabus.");
        return topics.save(new SyllabusTopic(unit, title, description, effortMinutes, important,
                topics.findMaxOrder(unitId, teacherId) + 1));
    }

    @Transactional
    public SyllabusTopic updateTopic(String subjectId, String unitId, String topicId, String teacherId,
                                    String submittedTitle, String submittedDescription,
                                    int effortMinutes, boolean important) {
        requireEditableSubject(subjectId, teacherId);
        SyllabusTopic topic = getTopic(subjectId, unitId, topicId, teacherId);
        String title = requireText(submittedTitle, TOPIC_TITLE_LIMIT, "topic title", 180);
        String description = normalizeDescription(submittedDescription);
        validateEffort(effortMinutes);
        topic.update(title, description, effortMinutes, important);
        return topic;
    }

    @Transactional
    public boolean moveTopic(String subjectId, String unitId, String topicId, String teacherId, String direction) {
        requireEditableSubject(subjectId, teacherId);
        getUnitForSubject(unitId, subjectId, teacherId);
        List<SyllabusTopic> ordered = topics.findAllForTeacher(unitId, teacherId);
        int index = indexOfTopic(ordered, topicId);
        int destination = index + directionOffset(direction);
        if (index < 0 || destination < 0 || destination >= ordered.size()) return false;
        SyllabusTopic current = ordered.get(index);
        SyllabusTopic neighbor = ordered.get(destination);
        int currentOrder = current.getSortOrder();
        current.setSortOrder(neighbor.getSortOrder());
        neighbor.setSortOrder(currentOrder);
        return true;
    }

    private Subject requireEditableSubject(String subjectId, String teacherId) {
        Subject subject = subjects.getForTeacher(subjectId, teacherId);
        if (subject.isArchived()) {
            throw new InvalidPlannerInputException("Restore this subject before editing its syllabus.");
        }
        return subject;
    }

    private String requireText(String value, int limit, String label, int displayedLimit) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank() || text.length() > limit) {
            throw new InvalidPlannerInputException("Enter a " + label + " between 1 and " + displayedLimit + " characters.");
        }
        return text;
    }

    private String normalizeDescription(String value) {
        String description = value == null ? "" : value.trim();
        if (description.length() > DESCRIPTION_LIMIT) {
            throw new InvalidPlannerInputException("Description must be 2,000 characters or fewer.");
        }
        return description.isEmpty() ? null : description;
    }

    private void validateEffort(int minutes) {
        if (minutes < 1 || minutes > MAX_EFFORT_MINUTES) {
            throw new InvalidPlannerInputException("Estimated effort must be between 1 and 10,080 minutes.");
        }
    }

    private int directionOffset(String direction) {
        return switch (direction == null ? "" : direction.toLowerCase(java.util.Locale.ROOT)) {
            case "up" -> -1;
            case "down" -> 1;
            default -> throw new InvalidPlannerInputException("Choose whether to move the item up or down.");
        };
    }

    private int indexOfUnit(List<SyllabusUnit> ordered, String unitId) {
        for (int i = 0; i < ordered.size(); i++) if (ordered.get(i).getId().equals(unitId)) return i;
        return -1;
    }

    private int indexOfTopic(List<SyllabusTopic> ordered, String topicId) {
        for (int i = 0; i < ordered.size(); i++) if (ordered.get(i).getId().equals(topicId)) return i;
        return -1;
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Syllabus item not found.");
    }
}
