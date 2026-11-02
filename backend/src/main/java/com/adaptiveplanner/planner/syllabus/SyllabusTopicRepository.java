package com.adaptiveplanner.planner.syllabus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SyllabusTopicRepository extends JpaRepository<SyllabusTopic, String> {
    @Query("select t from SyllabusTopic t where t.unit.subject.id = :subjectId "
            + "and t.unit.subject.teacher.id = :teacherId order by t.unit.sortOrder, t.sortOrder, t.title")
    List<SyllabusTopic> findAllForSubjectAndTeacher(@Param("subjectId") String subjectId,
                                                    @Param("teacherId") String teacherId);

    @Query("select t from SyllabusTopic t where t.unit.id = :unitId "
            + "and t.unit.subject.teacher.id = :teacherId order by t.sortOrder, t.title")
    List<SyllabusTopic> findAllForTeacher(@Param("unitId") String unitId,
                                          @Param("teacherId") String teacherId);

    @Query("select t from SyllabusTopic t where t.id = :topicId and t.unit.id = :unitId "
            + "and t.unit.subject.teacher.id = :teacherId")
    Optional<SyllabusTopic> findForUnitAndTeacher(@Param("topicId") String topicId,
                                                  @Param("unitId") String unitId,
                                                  @Param("teacherId") String teacherId);

    @Query("select coalesce(max(t.sortOrder), 0) from SyllabusTopic t where t.unit.id = :unitId "
            + "and t.unit.subject.teacher.id = :teacherId")
    int findMaxOrder(@Param("unitId") String unitId, @Param("teacherId") String teacherId);
}
