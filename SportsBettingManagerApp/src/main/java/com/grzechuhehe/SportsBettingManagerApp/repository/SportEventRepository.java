package com.grzechuhehe.SportsBettingManagerApp.repository;

import com.grzechuhehe.SportsBettingManagerApp.model.SportEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SportEventRepository extends JpaRepository<SportEvent, Long> {
    Optional<SportEvent> findByTeamHomeAndTeamAwayAndDateAndSportType(
            String teamHome,
            String teamAway,
            LocalDateTime date,
            SportEvent.SportType sportType
    );
}