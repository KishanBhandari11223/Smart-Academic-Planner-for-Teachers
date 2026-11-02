package com.adaptiveplanner.planner.workspace;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlannerWorkspaceRepository extends JpaRepository<PlannerWorkspace, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select workspace from PlannerWorkspace workspace where workspace.teacherId = :teacherId")
    Optional<PlannerWorkspace> findByTeacherIdForUpdate(@Param("teacherId") String teacherId);
}
