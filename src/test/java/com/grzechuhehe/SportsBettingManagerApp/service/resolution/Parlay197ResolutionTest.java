package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Regresja na realnym kuponie #197 — część nóg powinna się rozliczyć gdy Apify zwróci wyniki.
 */
@ExtendWith(MockitoExtension.class)
class Parlay197ResolutionTest {

    @Mock private BetRepository betRepository;

    private BetResolutionTransactionService service;

    @BeforeEach
    void setUp() {
        service = ResolutionTestFixtures.transactionService(betRepository);
    }

    @Test
    void shouldSettleNationalTeamLegsWhenScraperReturnsFinishedMatches() {
        LocalDateTime placed = LocalDateTime.of(2026, 6, 7, 18, 38, 38);
        Bet parlay = Bet.builder()
                .id(197L).betType(BetType.PARLAY).status(BetStatus.PENDING)
                .stake(new BigDecimal("10")).placedAt(placed)
                .build();

        Bet croatia = leg(200L, "Chorwacja - Słowenia", "Chorwacja", MarketType.MONEYLINE_1X2, parlay, placed);
        Bet morocco = leg(201L, "Maroko - Norwegia",
                "BetBuilder: Suma: powyżej 1.5, Handicap 0:2: Norwegia (0:2)",
                MarketType.TOTALS_OVER_UNDER, parlay, placed);
        Bet brazilTotals = leg(204L, "Brazylia (K) - Włochy (K)", "powyżej 3.5",
                MarketType.TOTALS_OVER_UNDER, parlay, placed);

        parlay.setChildBets(new LinkedHashSet<>(List.of(croatia, morocco, brazilTotals)));

        List<SofaScoreEventDto> pool = List.of(
                finished("Croatia", "Slovenia", 2, 0, placed.plusHours(2)),
                finished("Morocco", "Norway", 2, 1, placed.plusHours(3)),
                finished("Brazil", "Italy", 2, 2, placed.plusHours(4))
        );

        when(betRepository.findByIdWithChildBets(197L)).thenReturn(java.util.Optional.of(parlay));

        service.processRoot(
                197L, pool, LocalDateTime.of(2026, 6, 26, 11, 0),
                Set.of(200L, 201L, 204L), Set.of(200L, 201L, 204L), 0.85, 4, null);

        assertEquals(BetStatus.WON, croatia.getStatus());
        assertEquals(BetStatus.WON, morocco.getStatus(), "BetBuilder: over 1.5 + handicap Norwegia 0:2");
        assertEquals(BetStatus.WON, brazilTotals.getStatus(), "4 bramek = over 3.5 wygrany");
    }

    @Test
    void shouldSettleExtendedParlay197Legs() {
        LocalDateTime placed = LocalDateTime.of(2026, 6, 7, 18, 38, 38);
        Bet parlay = Bet.builder()
                .id(197L).betType(BetType.PARLAY).status(BetStatus.PENDING)
                .stake(new BigDecimal("10")).placedAt(placed)
                .build();

        Bet usaWomen = leg(199L, "USA (K) - Niemcy (K)", "USA (K)", MarketType.MONEYLINE_1X2, parlay, placed);
        Bet zverevHandicap = leg(203L, "Cobolli, Flavio - Zverev, Alexander",
                "Zverev, Alexander (-1.5)", MarketType.HANDICAP, parlay, placed);
        zverevHandicap.setLine("-1.5");
        zverevHandicap.setSport("Tennis");

        parlay.setChildBets(new LinkedHashSet<>(List.of(usaWomen, zverevHandicap)));

        SofaScoreEventDto usaEvent = finished("United States", "Germany", 2, 1, placed.plusHours(1));
        usaEvent.setTournament("International Friendly Women");
        List<SofaScoreEventDto> pool = List.of(
                usaEvent,
                tennisFinished("Cobolli, Flavio", "Zverev, Alexander", 4, 6, placed.plusHours(5))
        );

        when(betRepository.findByIdWithChildBets(197L)).thenReturn(java.util.Optional.of(parlay));

        service.processRoot(
                197L, pool, LocalDateTime.of(2026, 6, 26, 11, 0),
                Set.of(199L, 203L), Set.of(199L, 203L), 0.85, 4, null);

        assertEquals(BetStatus.WON, usaWomen.getStatus());
        assertEquals(BetStatus.WON, zverevHandicap.getStatus(), "Zverev -1.5 gemów vs 4-6");
    }

    @Test
    void allParlay197EventNamesShouldBeSearchable() {
        ResolutionNameTranslator translator = new ResolutionNameTranslator();
        List<String> events = List.of(
                "LZS Krynki - Pionier Brańsk",
                "USA (K) - Niemcy (K)",
                "Chorwacja - Słowenia",
                "Maroko - Norwegia",
                "Piaskowianka Piaski - Granat Skarżysko-Kamienna",
                "Cobolli, Flavio - Zverev, Alexander",
                "Brazylia (K) - Włochy (K)"
        );
        for (String event : events) {
            assertTrue(translator.isSearchableEventName(event), event);
        }
    }

    private static Bet leg(
            Long id, String eventName, String selection, MarketType market, Bet parlay, LocalDateTime placed) {
        return Bet.builder()
                .id(id).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .eventName(eventName).selection(selection).marketType(market)
                .parentBet(parlay).placedAt(placed)
                .build();
    }

    private static SofaScoreEventDto finished(
            String home, String away, int homeScore, int awayScore, LocalDateTime start) {
        SofaScoreEventDto e = new SofaScoreEventDto();
        e.setType("match");
        e.setStatusType("finished");
        e.setHomeTeam(home);
        e.setAwayTeam(away);
        e.setHomeScore(homeScore);
        e.setAwayScore(awayScore);
        e.setStartTimestamp(start.toEpochSecond(ZoneOffset.UTC));
        return e;
    }

    private static SofaScoreEventDto tennisFinished(
            String home, String away, int homeScore, int awayScore, LocalDateTime start) {
        SofaScoreEventDto e = finished(home, away, homeScore, awayScore, start);
        e.setSport("tennis");
        return e;
    }
}
