package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompositeSelectionParserTest {

    private final CompositeSelectionParser parser = new CompositeSelectionParser(new ResolutionNameTranslator());

    @Test
    void parsesMaroccoNorwayBetBuilder() {
        var conditions = parser.parse(
                "BetBuilder: Suma: powyżej 1.5, Handicap 0:2: Norwegia (0:2)");
        assertEquals(2, conditions.size());
        assertEquals(MarketType.TOTALS_OVER_UNDER, conditions.get(0).marketType());
        assertEquals(MarketType.HANDICAP, conditions.get(1).marketType());
        assertEquals("0:2", conditions.get(1).line());
    }
}
