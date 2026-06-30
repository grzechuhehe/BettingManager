package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.DoublesNameNormalizer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExtendedMarketResolverTest {

    private final StandardMarketResolver standard =
            new StandardMarketResolver(new ResolutionNameTranslator(new DoublesNameNormalizer()));
    private final StatisticsMarketResolver statistics =
            new StatisticsMarketResolver(new ResolutionNameTranslator(new DoublesNameNormalizer()));

    @Test
    void doubleChanceHomeOrDrawWon() {
        Bet bet = Bet.builder()
                .marketType(MarketType.DOUBLE_CHANCE)
                .selection("Chorwacja lub remis")
                .build();
        SofaScoreEventDto e = finished("Croatia", "Ghana", 1, 1);
        assertEquals(Optional.of(BetStatus.WON), standard.resolve(bet, e));
    }

    @Test
    void teamTotalGoalsOverWon() {
        Bet bet = Bet.builder()
                .marketType(MarketType.TEAM_TOTAL_GOALS)
                .selection("Senegal Over 1.5")
                .build();
        SofaScoreEventDto e = finished("Senegal", "Iraq", 2, 0);
        assertEquals(Optional.of(BetStatus.WON), standard.resolve(bet, e));
    }

    @Test
    void teamTotalShotsResolvedFromStatistics() {
        Bet bet = Bet.builder()
                .marketType(MarketType.TEAM_TOTAL_SHOTS)
                .selection("Iraq Over 4.5")
                .build();
        SofaScoreEventDto e = finished("Senegal", "Iraq", 1, 0);
        e.setStatistics(Map.of(
                "home", Map.of("shots", 8),
                "away", Map.of("shots", 6)));
        assertEquals(Optional.of(BetStatus.WON), statistics.resolve(bet, e));
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
