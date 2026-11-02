package com.adaptiveplanner.planner.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeachingPlanDayRepository extends JpaRepository<TeachingPlanDay, String> {
    @Query("select d from TeachingPlanDay d where d.plan.id = :planId "
            + "and d.plan.section.subject.teacher.id = :teacherId order by d.dayOfWeek")
    List<TeachingPlanDay> findAllForPlan(@Param("planId") String planId,
                                         @Param("teacherId") String teacherId);
}
