package com.adaptiveplanner.planner.schedule;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleAdjustmentProposalRepository extends JpaRepository<ScheduleAdjustmentProposal, String> {
    @EntityGraph(attributePaths = {"plan", "plan.section", "plan.section.subject", "sourceSession"})
    @Query("select p from ScheduleAdjustmentProposal p where p.plan.section.subject.teacher.id = :teacherId "
            + "and p.status = :status order by p.proposedDate, p.startTime")
    List<ScheduleAdjustmentProposal> findAllForTeacherAndStatus(@Param("teacherId") String teacherId,
                                                                 @Param("status") AdjustmentProposalStatus status);

    @EntityGraph(attributePaths = {"plan", "plan.section", "plan.section.subject", "sourceSession", "topic"})
    @Query("select p from ScheduleAdjustmentProposal p where p.id = :proposalId "
            + "and p.plan.section.subject.teacher.id = :teacherId")
    Optional<ScheduleAdjustmentProposal> findOwned(@Param("proposalId") String proposalId,
                                                    @Param("teacherId") String teacherId);
}
