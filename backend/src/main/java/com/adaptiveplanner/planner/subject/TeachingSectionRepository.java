package com.adaptiveplanner.planner.subject;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface TeachingSectionRepository extends JpaRepository<TeachingSection, String> {
    @Query("select new com.adaptiveplanner.planner.subject.SubjectSectionCount(s.subject.id, count(s)) "
            + "from TeachingSection s where s.subject.teacher.id = :teacherId and s.subject.id in :subjectIds "
            + "and s.archived = false group by s.subject.id")
    List<SubjectSectionCount> countActiveSectionsForSubjects(@Param("teacherId") String teacherId,
                                                              @Param("subjectIds") Collection<String> subjectIds);

    @EntityGraph(attributePaths = "weeklySlots")
    @Query("select distinct s from TeachingSection s where s.subject.id = :subjectId and s.subject.teacher.id = :teacherId "
            + "and s.archived = :archived order by s.name")
    List<TeachingSection> findSectionsForTeacher(@Param("subjectId") String subjectId,
                                                  @Param("teacherId") String teacherId,
                                                  @Param("archived") boolean archived);

    @EntityGraph(attributePaths = {"weeklySlots", "subject"})
    @Query("select s from TeachingSection s where s.id = :sectionId and s.subject.teacher.id = :teacherId")
    Optional<TeachingSection> findOwnedWithSlots(@Param("sectionId") String sectionId,
                                                 @Param("teacherId") String teacherId);

    @Query("select s.subject.id from TeachingSection s where s.id = :sectionId and s.subject.teacher.id = :teacherId")
    Optional<String> findSubjectIdForTeacher(@Param("sectionId") String sectionId,
                                              @Param("teacherId") String teacherId);

    @Query("select case when count(s) > 0 then true else false end from TeachingSection s where s.subject.id = :subjectId "
            + "and s.subject.teacher.id = :teacherId and lower(s.name) = lower(:name) and s.id <> :excludeId")
    boolean existsOtherName(@Param("subjectId") String subjectId, @Param("teacherId") String teacherId,
                            @Param("name") String name, @Param("excludeId") String excludeId);

    @Query("select case when count(s) > 0 then true else false end from TeachingSection s where s.subject.id = :subjectId "
            + "and s.subject.teacher.id = :teacherId and lower(s.name) = lower(:name)")
    boolean existsName(@Param("subjectId") String subjectId, @Param("teacherId") String teacherId,
                       @Param("name") String name);
}
