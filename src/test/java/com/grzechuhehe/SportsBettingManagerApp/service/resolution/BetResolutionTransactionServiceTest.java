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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetResolutionTransactionServiceTest {

    @Mock private BetRepository betRepository;

    private BetResolutionTransactionService service;
    private ResolutionNameTranslator nameTranslator;

    @BeforeEach
    void setUp() {
        nameTranslator = new ResolutionNameTranslator();
        service = new BetResolutionTransactionService(
                betRepository,
                new BetMatcher(nameTranslator),
                new BetOutcomeEvaluator(),
                nameTranslator);
    }

    private SofaScoreEventDto finishedEvent(String home, String away, int homeScore, int awayScore, LocalDateTime start) {
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

    private SofaScoreEventDto canceledEvent(String home, String away, LocalDateTime start) {
        SofaScoreEventDto e = new SofaScoreEventDto();
        e.setType("match");
        e.setStatusType("canceled");
        e.setHomeTeam(home);
        e.setAwayTeam(away);
        e.setStartTimestamp(start.toEpochSecond(ZoneOffset.UTC));
        return e;
    }

    @Test
    void shouldVoidParlayWhenAllLegsVoid() {
        LocalDateTime placed = LocalDateTime.of(2026, 6, 1, 12, 0);
        Bet parlay = Bet.builder()
                .id(400L).betType(BetType.PARLAY).status(BetStatus.PENDING)
                .stake(new BigDecimal("10")).odds(new BigDecimal("4.0"))
                .potentialWinnings(new BigDecimal("40"))
                .placedAt(placed)
                .build();
        Bet leg1 = Bet.builder()
                .id(401L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("Legia Warszawa")
                .eventName("Legia Warszawa - Lech Poznan")
                .parentBet(parlay).placedAt(placed)
                .build();
        Bet leg2 = Bet.builder()
                .id(402L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("Brighton")
                .eventName("Brighton - Manchester United")
                .parentBet(parlay).placedAt(placed)
                .build();
        parlay.setChildBets(new LinkedHashSet<>(List.of(leg1, leg2)));

        List<SofaScoreEventDto> pool = List.of(
                canceledEvent("Legia Warszawa", "Lech Poznan", placed.plusDays(1)),
                canceledEvent("Brighton", "Manchester United", placed.plusDays(1))
        );

        when(betRepository.findByIdWithChildBets(400L)).thenReturn(java.util.Optional.of(parlay));

        service.processRoot(400L, pool, LocalDateTime.of(2026, 6, 5, 12, 0), Set.of(401L, 402L), 0.85, 4);

        assertEquals(BetStatus.VOID, leg1.getStatus());
        assertEquals(BetStatus.VOID, leg2.getStatus());
        assertEquals(BetStatus.VOID, parlay.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(parlay.getFinalProfit()));
    }

    @Test
    void shouldRecalculateParlayOddsWhenSomeLegsVoid() {
        LocalDateTime placed = LocalDateTime.of(2026, 6, 1, 12, 0);
        Bet parlay = Bet.builder()
                .id(500L).betType(BetType.PARLAY).status(BetStatus.PENDING)
                .stake(new BigDecimal("10")).odds(new BigDecimal("6.0"))
                .potentialWinnings(new BigDecimal("60"))
                .placedAt(placed)
                .build();
        Bet legWon = Bet.builder()
                .id(501L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("Legia Warszawa")
                .eventName("Legia Warszawa - Lech Poznan").odds(new BigDecimal("2.0"))
                .parentBet(parlay).placedAt(placed)
                .build();
        Bet legVoid = Bet.builder()
                .id(502L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("Brighton")
                .eventName("Brighton - Manchester United").odds(new BigDecimal("3.0"))
                .parentBet(parlay).placedAt(placed)
                .build();
        parlay.setChildBets(new LinkedHashSet<>(List.of(legWon, legVoid)));

        List<SofaScoreEventDto> pool = List.of(
                finishedEvent("Legia Warszawa", "Lech Poznan", 2, 1, placed.plusDays(1)),
                canceledEvent("Brighton", "Manchester United", placed.plusDays(1))
        );

        when(betRepository.findByIdWithChildBets(500L)).thenReturn(java.util.Optional.of(parlay));

        service.processRoot(500L, pool, LocalDateTime.of(2026, 6, 5, 12, 0), Set.of(501L, 502L), 0.85, 4);

        assertEquals(BetStatus.WON, legWon.getStatus());
        assertEquals(BetStatus.VOID, legVoid.getStatus());
        assertEquals(BetStatus.WON, parlay.getStatus());
        // Kurs liczony tylko z nogi wygranej (2.0): wygrana 10*2=20, zysk 10.
        assertEquals(0, new BigDecimal("20").compareTo(parlay.getPotentialWinnings()));
        assertEquals(0, new BigDecimal("10").compareTo(parlay.getFinalProfit()));
    }

    @Test
    void shouldSettleParlayOnlyWhenAllLegsWon() {
        LocalDateTime placed = LocalDateTime.of(2026, 6, 1, 12, 0);
        Bet parlay = Bet.builder()
                .id(100L).betType(BetType.PARLAY).status(BetStatus.PENDING)
                .stake(new BigDecimal("10")).odds(new BigDecimal("4.0"))
                .potentialWinnings(new BigDecimal("40"))
                .placedAt(placed)
                .build();

        Bet leg1 = Bet.builder()
                .id(101L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("Legia Warszawa")
                .eventName("Legia Warszawa - Lech Poznan")
                .parentBet(parlay).placedAt(placed)
                .build();
        Bet leg2 = Bet.builder()
                .id(102L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("Brighton")
                .eventName("Brighton - Manchester United")
                .parentBet(parlay).placedAt(placed)
                .build();

        parlay.setChildBets(new LinkedHashSet<>(List.of(leg1, leg2)));

        List<SofaScoreEventDto> pool = List.of(
                finishedEvent("Legia Warszawa", "Lech Poznan", 2, 1, placed.plusDays(1)),
                finishedEvent("Brighton", "Manchester United", 1, 0, placed.plusDays(1))
        );

        when(betRepository.findByIdWithChildBets(100L)).thenReturn(java.util.Optional.of(parlay));

        service.processRoot(
                100L,
                pool,
                LocalDateTime.of(2026, 6, 5, 12, 0),
                Set.of(101L, 102L),
                0.85,
                4
        );

        assertEquals(BetStatus.WON, leg1.getStatus());
        assertEquals(BetStatus.WON, leg2.getStatus());
        assertEquals(BetStatus.WON, parlay.getStatus());
        verify(betRepository, atLeast(3)).save(any(Bet.class));
    }

    @Test
    void shouldKeepParlayPendingUntilAllLegsDecided() {
        LocalDateTime placed = LocalDateTime.of(2026, 6, 1, 12, 0);
        Bet parlay = Bet.builder()
                .id(200L).betType(BetType.PARLAY).status(BetStatus.PENDING)
                .stake(new BigDecimal("10"))
                .placedAt(placed)
                .build();

        Bet leg1 = Bet.builder()
                .id(201L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("Legia Warszawa")
                .eventName("Legia Warszawa - Lech Poznan")
                .parentBet(parlay).placedAt(placed)
                .build();
        Bet leg2 = Bet.builder()
                .id(202L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("X")
                .eventName("Completely Unknown - Another Team")
                .parentBet(parlay).placedAt(placed)
                .build();

        parlay.setChildBets(new LinkedHashSet<>(List.of(leg1, leg2)));

        when(betRepository.findByIdWithChildBets(200L)).thenReturn(java.util.Optional.of(parlay));

        service.processRoot(
                200L,
                List.of(finishedEvent("Legia Warszawa", "Lech Poznan", 2, 1, placed.plusDays(1))),
                LocalDateTime.of(2026, 6, 5, 12, 0),
                Set.of(201L, 202L),
                0.85,
                4
        );

        assertEquals(BetStatus.WON, leg1.getStatus());
        assertEquals(BetStatus.PENDING, leg2.getStatus());
        assertEquals(BetStatus.PENDING, parlay.getStatus());
        verify(betRepository, never()).save(parlay);
    }

    @Test
    void shouldLoseParlayWhenAnyLegLost() {
        LocalDateTime placed = LocalDateTime.of(2026, 6, 1, 12, 0);
        Bet parlay = Bet.builder()
                .id(300L).betType(BetType.PARLAY).status(BetStatus.PENDING)
                .stake(new BigDecimal("10"))
                .placedAt(placed)
                .build();

        Bet leg1 = Bet.builder()
                .id(301L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("Lech Poznan")
                .eventName("Legia Warszawa - Lech Poznan")
                .parentBet(parlay).placedAt(placed)
                .build();
        Bet leg2 = Bet.builder()
                .id(302L).betType(BetType.SINGLE).status(BetStatus.WON)
                .marketType(MarketType.MONEYLINE_1X2).selection("Brighton")
                .eventName("Brighton - Manchester United")
                .parentBet(parlay).placedAt(placed)
                .build();

        parlay.setChildBets(new LinkedHashSet<>(List.of(leg1, leg2)));

        when(betRepository.findByIdWithChildBets(300L)).thenReturn(java.util.Optional.of(parlay));

        service.processRoot(
                300L,
                List.of(finishedEvent("Legia Warszawa", "Lech Poznan", 2, 1, placed.plusDays(1))),
                LocalDateTime.of(2026, 6, 5, 12, 0),
                Set.of(301L),
                0.85,
                4
        );

        assertEquals(BetStatus.LOST, leg1.getStatus());
        assertEquals(BetStatus.LOST, parlay.getStatus());
        verify(betRepository).save(parlay);
    }
}
