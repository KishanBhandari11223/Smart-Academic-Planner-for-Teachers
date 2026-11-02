package com.adaptiveplanner.planner.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, String> {
    List<OtpChallenge> findByEmailOrderByCreatedAtDesc(String email);

    Optional<OtpChallenge> findTopByEmailAndConsumedAtIsNullOrderByCreatedAtDesc(String email);

    long countByEmailAndCreatedAtAfter(String email, Instant createdAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OtpChallenge c set c.consumedAt = :now where c.email = :email and c.consumedAt is null")
    int invalidateOutstanding(@Param("email") String email, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OtpChallenge c set c.attempts = c.attempts + 1 where c.id = :id and c.consumedAt is null and c.attempts < :maxAttempts and c.expiresAt > :now")
    int recordFailedAttempt(@Param("id") String id, @Param("maxAttempts") int maxAttempts, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OtpChallenge c set c.consumedAt = :now where c.id = :id and c.consumedAt is null and c.attempts < :maxAttempts and c.expiresAt > :now")
    int consumeIfActive(@Param("id") String id, @Param("maxAttempts") int maxAttempts, @Param("now") Instant now);
}
