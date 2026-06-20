package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.ApifySofaScoreClient;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetResolutionServiceTest {

    @Mock private BetRepository betRepository;
    @Mock private ApifySofaScoreClient apifySofaScoreClient;

    private BetResolutionService service;

    @BeforeEach
    void setUp() {
        BetMatcher matcher = new BetMatcher();
        BetOutcomeEvaluator evaluator = new BetOutcomeEvaluator();
        service = new BetResolutionService(betRepository, apifySofaScoreClient, matcher, evaluator);
        ReflectionTestUtils.setField(service, "confidenceThreshold", 0.85);
        ReflectionTestUtils.setField(service, "dateWindowDays", 4);
        ReflectionTestUtils.setField(service, "maxBetsPerRun", 50);
    }

    private SofaScoreEventDto finishedEvent(LocalDateTime start) {
        SofaScoreEventDto e = new SofaScoreEventDto();
        e.setType("match");
        e.setStatusType("finished");
        e.setHomeTeam("Legia Warszawa");
        e.setAwayTeam("Lech Poznan");
        e.setHomeScore(2);
        e.setAwayScore(1);
        e.setStartTimestamp(start.toEpochSecond(ZoneOffset.UTC));
        e.setUrl("https://www.sofascore.com/x#id:1");
        return e;
    }

    @Test
    void shouldSettleSingleWonBet() {
        LocalDateTime placed = LocalDateTime.of(2026, 5, 1, 12, 0);
        Bet bet = Bet.builder()
                .id(1L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("Legia Warszawa")
                .eventName("Legia Warszawa vs Lech Poznan")
                .stake(new BigDecimal("10")).odds(new BigDecimal("2.00"))
                .potentialWinnings(new BigDecimal("20")).placedAt(placed)
                .build();

        when(betRepository.findByStatusAndParentBetIsNull(BetStatus.PENDING)).thenReturn(List.of(bet));
        when(apifySofaScoreClient.searchMatches(anyString()))
                .thenReturn(List.of(finishedEvent(placed.plusDays(1))));

        service.resolvePendingBets();

        assertEquals(BetStatus.WON, bet.getStatus());
        assertEquals(0, new BigDecimal("10").compareTo(bet.getFinalProfit()));
        assertEquals("APIFY_SOFASCORE", bet.getResolutionSource());
        assertNotNull(bet.getSettledAt());
        verify(betRepository).save(bet);
    }

    @Test
    void shouldLeavePendingWhenNoMatch() {
        Bet bet = Bet.builder()
                .id(2L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("Legia")
                .eventName("Completely Different Team vs Another One")
                .stake(new BigDecimal("10")).odds(new BigDecimal("2.00"))
                .placedAt(LocalDateTime.of(2026, 5, 1, 12, 0))
                .build();

        when(betRepository.findByStatusAndParentBetIsNull(BetStatus.PENDING)).thenReturn(List.of(bet));
        when(apifySofaScoreClient.searchMatches(anyString())).thenReturn(List.of());

        service.resolvePendingBets();

        assertEquals(BetStatus.PENDING, bet.getStatus());
        verify(betRepository, never()).save(any());
    }
}
