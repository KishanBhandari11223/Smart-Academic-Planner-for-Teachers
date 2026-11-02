package com.adaptiveplanner.planner.syllabus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SyllabusUnitRepository extends JpaRepository<SyllabusUnit, String> {
    @EntityGraph(attributePaths = "topics")
    @Query("select distinct u from SyllabusUnit u where u.subject.id = :subjectId "
            + "and u.subject.teacher.id = :teacherId order by u.sortOrder, u.name")
    List<SyllabusUnit> findAllForTeacher(@Param("subjectId") String subjectId,
                                         @Param("teacherId") String teacherId);

    @Query("select u from SyllabusUnit u where u.id = :unitId and u.subject.teacher.id = :teacherId")
    Optional<SyllabusUnit> findOwned(@Param("unitId") String unitId, @Param("teacherId") String teacherId);

    @Query("select coalesce(max(u.sortOrder), 0) from SyllabusUnit u where u.subject.id = :subjectId "
            + "and u.subject.teacher.id = :teacherId")
    int findMaxOrder(@Param("subjectId") String subjectId, @Param("teacherId") String teacherId);

    @Query("select count(u) > 0 from SyllabusUnit u where u.subject.id = :subjectId "
            + "and u.subject.teacher.id = :teacherId and lower(u.name) = lower(:name)")
    boolean existsName(@Param("subjectId") String subjectId, @Param("teacherId") String teacherId,
                       @Param("name") String name);

    @Query("select count(u) > 0 from SyllabusUnit u where u.subject.id = :subjectId "
            + "and u.subject.teacher.id = :teacherId and lower(u.name) = lower(:name) and u.id <> :excludeId")
    boolean existsOtherName(@Param("subjectId") String subjectId, @Param("teacherId") String teacherId,
                             @Param("name") String name, @Param("excludeId") String excludeId);
}
