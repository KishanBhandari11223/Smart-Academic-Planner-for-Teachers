package com.adaptiveplanner.planner.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeachingPlanBlockedDateRepository extends JpaRepository<TeachingPlanBlockedDate, String> {
    @Query("select b from TeachingPlanBlockedDate b where b.plan.id = :planId "
            + "and b.plan.section.subject.teacher.id = :teacherId order by b.blockedDate")
    List<TeachingPlanBlockedDate> findAllForPlan(@Param("planId") String planId,
                                                  @Param("teacherId") String teacherId);

    @Query("select b from TeachingPlanBlockedDate b where b.plan.section.id = :sectionId "
            + "and b.plan.section.subject.teacher.id = :teacherId order by b.blockedDate")
    List<TeachingPlanBlockedDate> findAllForSection(@Param("sectionId") String sectionId,
                                                     @Param("teacherId") String teacherId);

    @Query("select b from TeachingPlanBlockedDate b where b.id = :blockedDateId and b.plan.id = :planId "
            + "and b.plan.section.subject.teacher.id = :teacherId")
    Optional<TeachingPlanBlockedDate> findOwned(@Param("blockedDateId") String blockedDateId,
                                                @Param("planId") String planId,
                                                @Param("teacherId") String teacherId);

    @Query("select case when count(b) > 0 then true else false end from TeachingPlanBlockedDate b "
            + "where b.plan.id = :planId and b.plan.section.subject.teacher.id = :teacherId "
            + "and b.blockedDate = :date")
    boolean existsDate(@Param("planId") String planId, @Param("teacherId") String teacherId,
                       @Param("date") LocalDate date);
}
