package com.grzechuhehe.SportsBettingManagerApp.service.resolution.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.market.CompositeSelectionParser;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.DoublesNameNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BetImportResolutionEnricherTest {

    private BetImportResolutionEnricher enricher;

    @BeforeEach
    void setUp() {
        ResolutionNameTranslator nameTranslator = new ResolutionNameTranslator(new DoublesNameNormalizer());
        enricher = new BetImportResolutionEnricher(
                new CompositeSelectionParser(),
                new ObjectMapper(),
                new MarketTypeInferrer(nameTranslator));
    }

    @Test
    void infersTotalsFromPowyzejSelection() {
        Bet bet = Bet.builder()
                .eventName("Real Madrid - Barcelona")
                .selection("Powyżej 2.5")
                .build();

        enricher.enrich(bet);

        assertEquals(MarketType.TOTALS_OVER_UNDER, bet.getMarketType());
    }

    @Test
    void setsOutrightWhenKeywordInEventOrSelection() {
        Bet bet = Bet.builder()
                .eventName("Mistrzostwa Świata 2026")
                .selection("Brazylia")
                .build();

        enricher.enrich(bet);

        assertEquals(MarketType.OUTRIGHT, bet.getMarketType());
    }

    @Test
    void serializesBuilderConditionsWhenMissing() {
        Bet bet = Bet.builder()
                .eventName("Maroko - Norwegia")
                .selection("BetBuilder: Suma: powyżej 1.5, Handicap 0:2: Norwegia (0:2)")
                .build();

        enricher.enrich(bet);

        assertNotNull(bet.getBuilderConditionsJson());
        assertTrue(bet.getBuilderConditionsJson().contains("TOTALS_OVER_UNDER"));
        assertTrue(bet.getBuilderConditionsJson().contains("HANDICAP"));
    }

    @Test
    void normalizesVsSeparatorInEventName() {
        Bet bet = Bet.builder()
                .eventName("Real Madrid vs Barcelona")
                .selection("1")
                .build();

        enricher.enrich(bet);

        assertEquals("Real Madrid - Barcelona", bet.getEventName());
    }
}
