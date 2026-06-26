package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SofaScoreSportMapperTest {

    private final SofaScoreSportMapper mapper = new SofaScoreSportMapper();

    @Test
    void shouldMapPolishFootball() {
        assertEquals("football", mapper.map("Piłka nożna").orElseThrow());
    }

    @Test
    void shouldMapFromBetList() {
        Bet bet = Bet.builder().sport("Koszykówka").build();
        List<String> sports = mapper.resolveSportsForBets(List.of(bet), List.of("football"));
        assertEquals(List.of("basketball"), sports);
    }

    @Test
    void shouldMapTennisFromPolish() {
        assertEquals("tennis", mapper.map("Tenis").orElseThrow());
    }

    @Test
    void shouldUseFallbackWhenSportUnknown() {
        Bet bet = Bet.builder().sport("Szachy").build();
        List<String> sports = mapper.resolveSportsForBets(List.of(bet), List.of("football", "tennis"));
        assertEquals(List.of("football", "tennis"), sports);
    }
}
