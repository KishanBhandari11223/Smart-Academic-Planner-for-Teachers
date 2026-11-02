package com.adaptiveplanner.planner.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherAccountRepository extends JpaRepository<TeacherAccount, String> {
    Optional<TeacherAccount> findByEmail(String email);
}