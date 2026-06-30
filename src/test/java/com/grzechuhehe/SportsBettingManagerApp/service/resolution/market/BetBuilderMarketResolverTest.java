package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.fasterxml.jackson.databind.ObjectMapper;
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

class BetBuilderMarketResolverTest {

    private BetBuilderMarketResolver resolver;

    @BeforeEach
    void setUp() {
        ResolutionNameTranslator translator = new ResolutionNameTranslator(new DoublesNameNormalizer(), new TennisNameNormalizer());
        CompositeSelectionParser parser = new CompositeSelectionParser();
        StandardMarketResolver standard = new StandardMarketResolver(translator);
        HandicapMarketResolver handicap = new HandicapMarketResolver(translator);
        StatisticsMarketResolver statistics = new StatisticsMarketResolver(translator);
        resolver = new BetBuilderMarketResolver(parser, standard, handicap, statistics, new ObjectMapper());
    }

    @Test
    void betBuilderWonWhenAllConditionsWon() {
        Bet bet = Bet.builder()
                .eventName("Maroko - Norwegia")
                .marketType(MarketType.TOTALS_OVER_UNDER)
                .selection("BetBuilder: Suma: powyżej 1.5, Handicap 0:2: Norwegia (0:2)")
                .build();
        SofaScoreEventDto event = finished("Morocco", "Norway", 2, 1);
        assertEquals(BetStatus.WON, resolver.resolve(bet, event).orElseThrow());
    }

    @Test
    void betBuilderLostWhenOneConditionLost() {
        Bet bet = Bet.builder()
                .eventName("Maroko - Norwegia")
                .marketType(MarketType.TOTALS_OVER_UNDER)
                .selection("BetBuilder: Suma: powyżej 1.5, Handicap 0:2: Norwegia (0:2)")
                .build();
        SofaScoreEventDto event = finished("Morocco", "Norway", 0, 0);
        assertEquals(BetStatus.LOST, resolver.resolve(bet, event).orElseThrow());
    }

    @Test
    void betBuilderEmptyWhenSelectionHasUnparseableFragment() {
        Bet bet = Bet.builder()
                .eventName("Maroko - Norwegia")
                .marketType(MarketType.TOTALS_OVER_UNDER)
                .selection("BetBuilder: Suma: powyżej 1.5, 1X2: Maroko, Handicap -1.5: Norwegia")
                .build();
        SofaScoreEventDto event = finished("Morocco", "Norway", 2, 1);
        assertTrue(resolver.resolve(bet, event).isEmpty(),
                "niepełny parse nie może dać WON — trafia do ręcznego rozliczenia");
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
