package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.MatchReRanker;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BetMatcherTest {

    private final BetMatcher matcher = new BetMatcher(new ResolutionNameTranslator(), new MatchReRanker());

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
    void shouldMatchTranslatedNationalTeamNamesAboveThreshold() {
        LocalDateTime placed = LocalDateTime.of(2026, 6, 7, 18, 0);
        Bet bet = Bet.builder()
                .eventName("Chorwacja - Słowenia")
                .placedAt(placed)
                .build();
        List<SofaScoreEventDto> events = List.of(
                event("Croatia", "Slovenia", placed.plusHours(2)));

        Optional<BetMatcher.MatchCandidate> result = matcher.findBestMatch(bet, events, 4);

        assertTrue(result.isPresent());
        assertTrue(result.get().confidence() >= 0.85);
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
    void shouldMatchWomensNationalTeamsAboveThreshold() {
        LocalDateTime placed = LocalDateTime.of(2026, 6, 7, 18, 0);
        Bet bet = Bet.builder()
                .eventName("Brazylia (K) - Włochy (K)")
                .placedAt(placed)
                .build();
        List<SofaScoreEventDto> events = List.of(
                event("Brazil", "Italy", placed.plusHours(4)));

        Optional<BetMatcher.MatchCandidate> result = matcher.findBestMatch(bet, events, 4);

        assertTrue(result.isPresent());
        assertTrue(result.get().confidence() >= 0.85);
    }

    @Test
    void shouldPreferWomensEventOverMensWhenBetIsWomens() {
        LocalDateTime placed = LocalDateTime.of(2026, 6, 7, 18, 0);
        Bet bet = Bet.builder()
                .eventName("USA (K) - Niemcy (K)")
                .placedAt(placed)
                .build();
        SofaScoreEventDto mens = event("United States", "Germany", placed.plusHours(2));
        mens.setTournament("International Friendly Men");
        SofaScoreEventDto womens = event("United States", "Germany", placed.plusHours(2));
        womens.setTournament("International Friendly Women");

        Optional<BetMatcher.MatchCandidate> result = matcher.findBestMatch(bet, List.of(mens, womens), 4);

        assertTrue(result.isPresent());
        assertEquals("International Friendly Women", result.get().event().getTournament());
        assertTrue(result.get().confidence() >= 0.85);
    }

    @Test
    void shouldReturnEmptyWhenNoEvents() {
        Bet bet = Bet.builder().eventName("A vs B").placedAt(LocalDateTime.now()).build();
        assertTrue(matcher.findBestMatch(bet, List.of(), 4).isEmpty());
    }
}
