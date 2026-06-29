package com.grzechuhehe.SportsBettingManagerApp.repository;

import com.grzechuhehe.SportsBettingManagerApp.model.BetResolutionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BetResolutionAttemptRepository extends JpaRepository<BetResolutionAttempt, Long> {

    List<BetResolutionAttempt> findTop10ByBetIdOrderByAttemptedAtDesc(Long betId);

    List<BetResolutionAttempt> findByCycleId(String cycleId);
}
