package com.adaptiveplanner.planner.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UnplannedTopicRepository extends JpaRepository<UnplannedTopic, String> {
    @Query("select u from UnplannedTopic u where u.plan.id = :planId "
            + "and u.plan.section.subject.teacher.id = :teacherId order by u.topicOrder")
    List<UnplannedTopic> findAllForPlan(@Param("planId") String planId,
                                        @Param("teacherId") String teacherId);

    @Modifying
    @Query("delete from UnplannedTopic u where u.plan.id = :planId "
            + "and u.plan.section.subject.teacher.id = :teacherId")
    int deleteAllForPlan(@Param("planId") String planId, @Param("teacherId") String teacherId);
}
