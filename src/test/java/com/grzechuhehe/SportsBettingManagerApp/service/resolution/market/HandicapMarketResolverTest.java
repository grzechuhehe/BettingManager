package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.DoublesNameNormalizer;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.TennisNameNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HandicapMarketResolverTest {

    private HandicapMarketResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new HandicapMarketResolver(new ResolutionNameTranslator(new DoublesNameNormalizer(), new TennisNameNormalizer()));
    }

    @Test
    void tennisGameHandicapMinusOnePointFiveWithApifySetScoreIsEmpty() {
        // Apify search zwraca sety (np. Tabur–Mochizuki 2:3), nie gemy — gemowy -1.5 → manual
        Bet bet = Bet.builder()
                .marketType(MarketType.HANDICAP)
                .selection("Mochizuki, Shintaro (-1.5)")
                .line("-1.5")
                .sport("Tennis")
                .build();
        SofaScoreEventDto event = tennisEvent("Clement Tabur", "Shintaro Mochizuki", 2, 3);
        assertTrue(resolver.resolve(bet, event).isEmpty());
    }

    @Test
    void tennisSetHandicapWholeNumberCanResolveOnApifySetScore() {
        Bet bet = Bet.builder()
                .marketType(MarketType.HANDICAP)
                .selection("Shintaro Mochizuki (+1)")
                .line("+1")
                .sport("Tennis")
                .build();
        SofaScoreEventDto event = tennisEvent("Shintaro Mochizuki", "Clement Tabur", 0, 2);
        assertEquals(BetStatus.LOST, resolver.resolve(bet, event).orElseThrow());
    }

    @Test
    void virtualStartHandicapAwayWon() {
        Bet bet = Bet.builder()
                .marketType(MarketType.HANDICAP)
                .selection("Norwegia")
                .line("0:2")
                .build();
        SofaScoreEventDto event = finished("Morocco", "Norway", 2, 1);
        assertEquals(BetStatus.WON, resolver.resolve(bet, event).orElseThrow());
    }

    @Test
    void tennisGameHandicapWithoutGamesDataIsEmpty() {
        Bet bet = Bet.builder()
                .marketType(MarketType.HANDICAP)
                .selection("Zverev, Alexander (-4.5)")
                .line("-4.5")
                .sport("Tennis")
                .build();
        SofaScoreEventDto event = tennisEvent("Cobolli", "Zverev", 0, 2);
        assertTrue(resolver.resolve(bet, event).isEmpty(),
                "handicap gemowy bez danych gemowych → ręczne rozliczenie");
    }

    private static SofaScoreEventDto tennisEvent(String home, String away, int homeScore, int awayScore) {
        SofaScoreEventDto e = finished(home, away, homeScore, awayScore);
        e.setSport("tennis");
        return e;
    }

    private static SofaScoreEventDto finished(String home, String away, int homeScore, int awayScore) {
        SofaScoreEventDto e = new SofaScoreEventDto();
        e.setStatusType("finished");
        e.setHomeTeam(home);
        e.setAwayTeam(away);
        e.setHomeScore(homeScore);
        e.setAwayScore(awayScore);
        return e;
    }
}
