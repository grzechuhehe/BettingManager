package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WomenMatchResolutionTest {

    private final BetMatcher matcher = ResolutionTestFixtures.components().matcher();

    @Test
    void shouldMatchUsaWomenVsGermanyWomen() {
        Bet bet = Bet.builder()
                .eventName("USA (K) - Niemcy (K)")
                .selection("USA (K)")
                .placedAt(LocalDateTime.of(2026, 6, 7, 18, 0))
                .build();
        SofaScoreEventDto event = finished("United States", "Germany", 2, 1);
        event.setTournament("International Friendly Women");

        Optional<BetMatcher.MatchCandidate> match = matcher.findBestMatch(bet, List.of(event), 4);

        assertTrue(match.isPresent());
        assertTrue(match.get().confidence() >= 0.85);
    }

    private static SofaScoreEventDto finished(String home, String away, int homeScore, int awayScore) {
        SofaScoreEventDto e = new SofaScoreEventDto();
        e.setStatusType("finished");
        e.setHomeTeam(home);
        e.setAwayTeam(away);
        e.setHomeScore(homeScore);
        e.setAwayScore(awayScore);
        e.setStartTimestamp(LocalDateTime.of(2026, 6, 7, 20, 0).toEpochSecond(ZoneOffset.UTC));
        return e;
    }
}
