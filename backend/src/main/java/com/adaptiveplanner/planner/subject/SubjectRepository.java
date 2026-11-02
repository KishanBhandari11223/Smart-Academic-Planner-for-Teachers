package com.adaptiveplanner.planner.subject;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, String> {
    List<Subject> findAllByTeacher_IdAndArchivedOrderByNameAsc(String teacherId, boolean archived);

    Optional<Subject> findByIdAndTeacher_Id(String id, String teacherId);

    Optional<Subject> findByTeacher_IdAndNameIgnoreCase(String teacherId, String name);
}
