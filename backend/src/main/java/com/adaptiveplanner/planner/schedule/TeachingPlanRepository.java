package com.adaptiveplanner.planner.schedule;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeachingPlanRepository extends JpaRepository<TeachingPlan, String> {
    @EntityGraph(attributePaths = {"section", "section.subject"})
    @Query("select p from TeachingPlan p where p.id = :planId and p.section.subject.teacher.id = :teacherId")
    Optional<TeachingPlan> findOwned(@Param("planId") String planId, @Param("teacherId") String teacherId);

    @Query("select p from TeachingPlan p where p.section.id = :sectionId "
            + "and p.section.subject.teacher.id = :teacherId order by p.createdAt desc")
    List<TeachingPlan> findAllForSection(@Param("sectionId") String sectionId,
                                         @Param("teacherId") String teacherId);
}
