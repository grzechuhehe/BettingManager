package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompositeSelectionParserTest {

    private final CompositeSelectionParser parser = new CompositeSelectionParser();

    @Test
    void parsesMaroccoNorwayBetBuilder() {
        var conditions = parser.parse(
                "BetBuilder: Suma: powyżej 1.5, Handicap 0:2: Norwegia (0:2)");
        assertEquals(2, conditions.size());
        assertEquals(MarketType.TOTALS_OVER_UNDER, conditions.get(0).marketType());
        assertEquals(MarketType.HANDICAP, conditions.get(1).marketType());
        assertEquals("0:2", conditions.get(1).line());
    }

    @Test
    void parsesNumericHandicapFragment() {
        var conditions = parser.parse("BetBuilder: Suma: powyżej 1.5, Handicap -1.5: Norwegia");
        assertEquals(2, conditions.size());
        assertEquals(MarketType.HANDICAP, conditions.get(1).marketType());
        assertEquals("-1.5", conditions.get(1).line());
        assertEquals("Norwegia", conditions.get(1).selection());
    }

    @Test
    void parseCompleteReturnsEmptyWhenAnyFragmentUnparseable() {
        var result = parser.parseComplete(
                "BetBuilder: Suma: powyżej 1.5, 1X2: Maroko, Handicap -1.5: Norwegia");
        assertTrue(result.isEmpty());
    }

    @Test
    void parseCompleteReturnsAllWhenEveryFragmentParseable() {
        var result = parser.parseComplete(
                "BetBuilder: Suma: powyżej 1.5, Handicap 0:2: Norwegia (0:2)");
        assertTrue(result.isPresent());
        assertEquals(2, result.get().size());
    }
}
