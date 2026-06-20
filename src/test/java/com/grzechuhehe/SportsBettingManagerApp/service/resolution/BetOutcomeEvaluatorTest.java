package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BetOutcomeEvaluatorTest {

    private final BetOutcomeEvaluator evaluator = new BetOutcomeEvaluator();

    private SofaScoreEventDto finished(int home, int away) {
        SofaScoreEventDto e = new SofaScoreEventDto();
        e.setStatusType("finished");
        e.setHomeTeam("Legia Warszawa");
        e.setAwayTeam("Lech Poznan");
        e.setHomeScore(home);
        e.setAwayScore(away);
        return e;
    }

    @Test
    void moneylineHomeWin() {
        Bet bet = Bet.builder().marketType(MarketType.MONEYLINE_1X2).selection("Legia Warszawa").build();
        assertEquals(Optional.of(BetStatus.WON), evaluator.evaluate(bet, finished(2, 1)));
    }

    @Test
    void moneylineHomeLossWhenDraw() {
        Bet bet = Bet.builder().marketType(MarketType.MONEYLINE_1X2).selection("Legia Warszawa").build();
        assertEquals(Optional.of(BetStatus.LOST), evaluator.evaluate(bet, finished(1, 1)));
    }

    @Test
    void totalsOverWon() {
        Bet bet = Bet.builder().marketType(MarketType.TOTALS_OVER_UNDER).selection("Over").line("2.5").build();
        assertEquals(Optional.of(BetStatus.WON), evaluator.evaluate(bet, finished(2, 1)));
    }

    @Test
    void totalsUnderLost() {
        Bet bet = Bet.builder().marketType(MarketType.TOTALS_OVER_UNDER).selection("Under 2.5").build();
        assertEquals(Optional.of(BetStatus.LOST), evaluator.evaluate(bet, finished(2, 1)));
    }

    @Test
    void bttsYesWon() {
        Bet bet = Bet.builder().marketType(MarketType.BOTH_TEAMS_TO_SCORE).selection("Yes").build();
        assertEquals(Optional.of(BetStatus.WON), evaluator.evaluate(bet, finished(2, 1)));
    }

    @Test
    void correctScoreWon() {
        Bet bet = Bet.builder().marketType(MarketType.CORRECT_SCORE).selection("2:1").build();
        assertEquals(Optional.of(BetStatus.WON), evaluator.evaluate(bet, finished(2, 1)));
    }

    @Test
    void canceledIsVoid() {
        Bet bet = Bet.builder().marketType(MarketType.MONEYLINE_1X2).selection("Legia Warszawa").build();
        SofaScoreEventDto e = finished(0, 0);
        e.setStatusType("canceled");
        assertEquals(Optional.of(BetStatus.VOID), evaluator.evaluate(bet, e));
    }

    @Test
    void notFinishedIsEmpty() {
        Bet bet = Bet.builder().marketType(MarketType.MONEYLINE_1X2).selection("Legia Warszawa").build();
        SofaScoreEventDto e = finished(0, 0);
        e.setStatusType("inprogress");
        assertTrue(evaluator.evaluate(bet, e).isEmpty());
    }

    @Test
    void unsupportedMarketIsEmpty() {
        Bet bet = Bet.builder().marketType(MarketType.PLAYER_PROPS).selection("anything").build();
        assertTrue(evaluator.evaluate(bet, finished(2, 1)).isEmpty());
    }
}
