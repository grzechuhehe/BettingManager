package com.grzechuhehe.SportsBettingManagerApp.repository;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus; // Nowy import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BetRepository extends JpaRepository<Bet, Long>{
    List<Bet> findByUser(User user);
    List<Bet> findByUserId(Long userId);
    Long countByUserAndStatus(User user, BetStatus status);
    @Query("SELECT COUNT(b) FROM Bet b WHERE b.user = :user AND b.status = :status AND b.isPreMatch = true AND b.parentBet IS NULL")
    Long countPreMatchByUserAndStatus(@Param("user") User user, @Param("status") BetStatus status);
    @Query("SELECT b FROM Bet b WHERE b.user = :user ORDER BY b.placedAt ASC")
    List<Bet> findByUserOrderByPlacedAtAsc(User user);
    @Query("SELECT b FROM Bet b WHERE b.user = :user AND b.placedAt >= :startDate")
    List<Bet> findByUserAndPeriod(User user, @Param("startDate") LocalDateTime startDate);
    
    Optional<Bet> findBySourcePostId(String sourcePostId);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Bet b WHERE b.user = :user AND b.isAiExtracted = true AND b.parentBet IS NULL")
    org.springframework.data.domain.Page<Bet> findRootAiBetsByUser(User user, org.springframework.data.domain.Pageable pageable);
}
