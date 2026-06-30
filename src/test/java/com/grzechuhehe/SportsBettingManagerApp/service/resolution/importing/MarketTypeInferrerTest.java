package com.grzechuhehe.SportsBettingManagerApp.service.resolution.importing;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.DoublesNameNormalizer;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.TennisNameNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketTypeInferrerTest {

    private MarketTypeInferrer inferrer;

    @BeforeEach
    void setUp() {
        ResolutionNameTranslator nameTranslator =
                new ResolutionNameTranslator(new DoublesNameNormalizer(), new TennisNameNormalizer());
        inferrer = new MarketTypeInferrer(nameTranslator);
    }

    @Test
    void inferFootballWithCommaIsNotTwoWay() {
        Bet bet = Bet.builder()
                .eventName("Independiente, Reserves - Racing")
                .sport("Football")
                .selection("1")
                .build();
        assertEquals(MarketType.MONEYLINE_1X2, inferrer.infer(bet));
    }

    @Test
    void inferExplicitTennisIsTwoWay() {
        Bet bet = Bet.builder()
                .eventName("Cobolli, Flavio - Zverev, Alexander")
                .sport("Tennis")
                .selection("Zverev, Alexander")
                .build();
        assertEquals(MarketType.MONEYLINE_12, inferrer.infer(bet));
    }

    @Test
    void inferNoSportTwoPlayerNamesIsTwoWay() {
        Bet bet = Bet.builder()
                .eventName("Cobolli, Flavio - Zverev, Alexander")
                .selection("Zverev, Alexander")
                .build();
        assertEquals(MarketType.MONEYLINE_12, inferrer.infer(bet));
    }

    @Test
    void inferNoSportSingleCommaIsNotTwoWay() {
        Bet bet = Bet.builder()
                .eventName("Independiente, Reserves - Racing")
                .selection("1")
                .build();
        assertEquals(MarketType.MONEYLINE_1X2, inferrer.infer(bet));
    }
}
