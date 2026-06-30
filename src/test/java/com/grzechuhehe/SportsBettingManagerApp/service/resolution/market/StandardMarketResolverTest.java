package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.DoublesNameNormalizer;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.TennisNameNormalizer;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StandardMarketResolverTest {

    private final StandardMarketResolver resolver =
            new StandardMarketResolver(new ResolutionNameTranslator(new DoublesNameNormalizer(), new TennisNameNormalizer()));

    @Test
    void moneyline12UsesWinnerCodeWhenPresent() {
        Bet bet = Bet.builder()
                .marketType(MarketType.MONEYLINE_12)
                .selection("Zverev, Alexander")
                .sport("Tennis")
                .build();
        SofaScoreEventDto e = new SofaScoreEventDto();
        e.setStatusType("finished");
        e.setSport("tennis");
        e.setHomeTeam("Cobolli, Flavio");
        e.setAwayTeam("Zverev, Alexander");
        e.setHomeScore(0);
        e.setAwayScore(0);
        e.setWinnerCode(2);
        assertEquals(Optional.of(BetStatus.WON), resolver.resolve(bet, e));
    }
}
