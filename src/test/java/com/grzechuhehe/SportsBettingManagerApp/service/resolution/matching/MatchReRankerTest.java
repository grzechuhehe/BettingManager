package com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.BetMatcher;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.DoublesNameNormalizer;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MatchReRankerTest {

    private final BetMatcher matcher = new BetMatcher(new ResolutionNameTranslator(new DoublesNameNormalizer()), new MatchReRanker());

    @Test
    void reranksWestHamLeedsAboveThreshold() {
        LocalDateTime placed = LocalDateTime.of(2026, 5, 10, 15, 0);
        Bet bet = Bet.builder()
                .eventName("West Ham - Leeds")
                .sport("Football")
                .placedAt(placed)
                .build();
        SofaScoreEventDto event = new SofaScoreEventDto();
        event.setHomeTeam("West Ham United");
        event.setAwayTeam("Leeds United");
        event.setStartTimestamp(placed.plusDays(1).toEpochSecond(ZoneOffset.UTC));

        Optional<BetMatcher.MatchCandidate> result = matcher.findBestMatch(bet, List.of(event), 4);

        assertTrue(result.isPresent());
        assertTrue(result.get().confidence() >= 0.85, "actual=" + result.get().confidence());
    }
}
