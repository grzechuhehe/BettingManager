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

@Repository
public interface BetRepository extends JpaRepository<Bet, Long>{
    List<Bet> findByUser(User user);
    Long countByUserAndStatus(User user, BetStatus status); // Zmieniono Bet.BetStatus na BetStatus
    @Query("SELECT b FROM Bet b WHERE b.user = :user ORDER BY b.placedAt ASC")
    List<Bet> findByUserOrderByPlacedAtAsc(User user);
    @Query("SELECT b FROM Bet b WHERE b.user = :user AND b.placedAt >= :startDate")
    List<Bet> findByUserAndPeriod(User user, @Param("startDate") LocalDateTime startDate);
}
