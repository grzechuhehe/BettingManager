package com.grzechuhehe.SportsBettingManagerApp.repository;

import com.grzechuhehe.SportsBettingManagerApp.model.BetResolutionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BetResolutionAttemptRepository extends JpaRepository<BetResolutionAttempt, Long> {

    List<BetResolutionAttempt> findTop10ByBetIdOrderByAttemptedAtDesc(Long betId);

    List<BetResolutionAttempt> findByCycleId(String cycleId);

    @Query("SELECT COUNT(a) FROM BetResolutionAttempt a WHERE a.errorCode = 'SUCCESS' AND a.attemptedAt >= :since")
    long countSuccessSince(@Param("since") LocalDateTime since);
}
