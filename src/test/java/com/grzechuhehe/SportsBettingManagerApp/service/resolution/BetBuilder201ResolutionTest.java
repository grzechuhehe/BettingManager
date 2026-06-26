package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BetBuilder201ResolutionTest {

    private final BetOutcomeEvaluator evaluator = ResolutionTestFixtures.components().evaluator();

    @Test
    void resolvesMoroccoNorwayBetBuilderFromMockEvent() {
        Bet bet = Bet.builder()
                .eventName("Maroko - Norwegia")
                .marketType(MarketType.TOTALS_OVER_UNDER)
                .selection("BetBuilder: Suma: powyżej 1.5, Handicap 0:2: Norwegia (0:2)")
                .build();
        SofaScoreEventDto event = new SofaScoreEventDto();
        event.setStatusType("finished");
        event.setHomeTeam("Morocco");
        event.setAwayTeam("Norway");
        event.setHomeScore(2);
        event.setAwayScore(1);

        assertEquals(BetStatus.WON, evaluator.evaluate(bet, event).orElseThrow());
    }
}
