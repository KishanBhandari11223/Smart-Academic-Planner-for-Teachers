package com.adaptiveplanner.planner.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlannedSessionRepository extends JpaRepository<PlannedSession, String> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"plan", "plan.section", "plan.section.subject", "topic"})
    @Query("select s from PlannedSession s where s.plan.status = :status "
            + "and s.plan.section.subject.teacher.id = :teacherId "
            + "order by s.plan.createdAt desc, s.scheduledDate, s.startTime, s.sortOrder")
    List<PlannedSession> findProgressSessions(@Param("teacherId") String teacherId,
                                              @Param("status") SchedulePlanStatus status);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"plan", "plan.section", "plan.section.subject"})
    @Query("select s from PlannedSession s where s.plan.status = :status "
            + "and s.plan.section.subject.teacher.id = :teacherId "
            + "and s.scheduledDate >= :fromDate and s.scheduledDate <= :toDate "
            + "order by s.scheduledDate, s.startTime, s.sortOrder")
    List<PlannedSession> findCalendarSessions(@Param("teacherId") String teacherId,
                                               @Param("status") SchedulePlanStatus status,
                                               @Param("fromDate") java.time.LocalDate fromDate,
                                               @Param("toDate") java.time.LocalDate toDate);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"plan", "plan.section", "plan.section.subject", "topic", "topic.unit"})
    @Query("select s from PlannedSession s where s.id = :sessionId "
            + "and s.plan.section.subject.teacher.id = :teacherId")
    java.util.Optional<PlannedSession> findOwnedForOutcome(@Param("sessionId") String sessionId,
                                                            @Param("teacherId") String teacherId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"plan", "plan.section", "plan.section.subject"})
    @Query("select s from PlannedSession s where s.plan.section.id = :sectionId "
            + "and s.plan.section.subject.teacher.id = :teacherId and s.plan.status = :status "
            + "order by s.scheduledDate, s.startTime, s.sortOrder")
    List<PlannedSession> findAllApprovedForSection(@Param("sectionId") String sectionId,
                                                   @Param("teacherId") String teacherId,
                                                   @Param("status") SchedulePlanStatus status);

    @Query("select s from PlannedSession s where s.plan.id = :planId "
            + "and s.plan.section.subject.teacher.id = :teacherId "
            + "order by s.scheduledDate, s.startTime, s.sortOrder")
    List<PlannedSession> findAllForPlan(@Param("planId") String planId,
                                        @Param("teacherId") String teacherId);

    @Modifying
    @Query("delete from PlannedSession s where s.plan.id = :planId "
            + "and s.plan.section.subject.teacher.id = :teacherId")
    int deleteAllForPlan(@Param("planId") String planId, @Param("teacherId") String teacherId);

    @Query("select count(s) from PlannedSession s where s.plan.id = :planId "
            + "and s.plan.section.subject.teacher.id = :teacherId")
    long countForPlan(@Param("planId") String planId, @Param("teacherId") String teacherId);
}
