package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BetMatcherTest {

    private final BetMatcher matcher = new BetMatcher();

    private SofaScoreEventDto event(String home, String away, LocalDateTime start) {
        SofaScoreEventDto e = new SofaScoreEventDto();
        e.setType("match");
        e.setHomeTeam(home);
        e.setAwayTeam(away);
        e.setStartTimestamp(start.toEpochSecond(ZoneOffset.UTC));
        return e;
    }

    @Test
    void shouldMatchByTeamNamesIgnoringDiacriticsWithinWindow() {
        LocalDateTime placed = LocalDateTime.of(2026, 5, 1, 12, 0);
        Bet bet = Bet.builder()
                .eventName("Legia Warszawa vs Lech Poznań")
                .placedAt(placed)
                .build();
        List<SofaScoreEventDto> events = List.of(
                event("Legia Warszawa", "Lech Poznan", placed.plusDays(1)));

        Optional<BetMatcher.MatchCandidate> result = matcher.findBestMatch(bet, events, 4);

        assertTrue(result.isPresent());
        assertEquals(1.0, result.get().confidence(), 0.0001);
    }

    @Test
    void shouldRejectEventOutsideDateWindow() {
        LocalDateTime placed = LocalDateTime.of(2026, 5, 1, 12, 0);
        Bet bet = Bet.builder()
                .eventName("Legia Warszawa vs Lech Poznań")
                .placedAt(placed)
                .build();
        List<SofaScoreEventDto> events = List.of(
                event("Legia Warszawa", "Lech Poznan", placed.plusDays(30)));

        assertTrue(matcher.findBestMatch(bet, events, 4).isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenNoEvents() {
        Bet bet = Bet.builder().eventName("A vs B").placedAt(LocalDateTime.now()).build();
        assertTrue(matcher.findBestMatch(bet, List.of(), 4).isEmpty());
    }
}
