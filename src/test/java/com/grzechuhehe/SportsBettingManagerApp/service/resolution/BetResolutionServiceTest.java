package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.discovery.DiscoveryResult;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.discovery.MatchDiscoveryService;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.discovery.ResolutionQueuePrioritizer;
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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetResolutionServiceTest {

    @Mock private BetRepository betRepository;
    @Mock private MatchDiscoveryService discoveryService;

    private BetResolutionService service;
    private BetResolutionTransactionService resolutionTx;
    private AutoResolutionGuard autoResolutionGuard;

    @BeforeEach
    void setUp() {
        ResolutionTestFixtures.ResolutionComponents c = ResolutionTestFixtures.components();
        resolutionTx = ResolutionTestFixtures.transactionService(betRepository);

        autoResolutionGuard = new AutoResolutionGuard();
        service = new BetResolutionService(
                c.nameTranslator(),
                resolutionTx,
                c.resolvabilityChecker(),
                autoResolutionGuard,
                new ResolutionQueuePrioritizer(),
                discoveryService);
        ReflectionTestUtils.setField(service, "confidenceThreshold", 0.85);
        ReflectionTestUtils.setField(service, "dateWindowDays", 4);
        ReflectionTestUtils.setField(service, "maxBetsPerRun", 50);
        ReflectionTestUtils.setField(service, "searchCooldownHours", 24);
        ReflectionTestUtils.setField(service, "minHoursAfterPlaced", 3);
        ReflectionTestUtils.setField(service, "manualCooldownMinutes", 60);

        lenient().when(discoveryService.getApifyMode()).thenReturn("scheduled");
        lenient().when(discoveryService.discover(eq(List.of()), any(LocalDateTime.class)))
                .thenReturn(DiscoveryResult.empty());
    }

    /** Synchronous test helper replacing the removed {@code resolvePendingBets(boolean)}. */
    private void runResolutionForced(boolean force) {
        AutoResolutionGuard.AcquireResult acquire = autoResolutionGuard.tryAcquire(60, true);
        assertEquals(AutoResolutionGuard.Acquisition.ACQUIRED, acquire.status());
        try {
            ReflectionTestUtils.invokeMethod(service, "resolvePendingBetsInternal", force);
        } finally {
            autoResolutionGuard.release(false);
        }
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

    private SofaScoreEventDto inprogressEvent(LocalDateTime start) {
        SofaScoreEventDto e = new SofaScoreEventDto();
        e.setType("match");
        e.setStatusType("inprogress");
        e.setHomeTeam("Legia Warszawa");
        e.setAwayTeam("Lech Poznan");
        e.setStartTimestamp(start.toEpochSecond(ZoneOffset.UTC));
        e.setUrl("https://www.sofascore.com/x#id:2");
        return e;
    }

    @Test
    void shouldNotSettleWhenDiscoveryReturnsNonTerminalEvent() {
        LocalDateTime placed = LocalDateTime.of(2026, 5, 1, 12, 0);
        Bet bet = Bet.builder()
                .id(21L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("Legia Warszawa")
                .eventName("Legia Warszawa - Lech Poznan")
                .stake(new BigDecimal("10")).odds(new BigDecimal("2.00"))
                .potentialWinnings(new BigDecimal("20")).placedAt(placed)
                .build();
        SofaScoreEventDto event = inprogressEvent(placed.plusDays(1));

        when(betRepository.findPendingRootIds(eq(BetStatus.PENDING), any())).thenReturn(List.of(bet.getId()));
        when(betRepository.findRootsWithLegsByIds(anyList())).thenReturn(List.of(bet));
        when(betRepository.findByIdWithChildBets(21L)).thenReturn(Optional.of(bet));
        when(discoveryService.discover(anyList(), any(LocalDateTime.class)))
                .thenReturn(new DiscoveryResult(List.of(event), 1, Set.of(21L), 0, 0));

        runResolutionForced(true);

        assertEquals(BetStatus.PENDING, bet.getStatus());
        verify(discoveryService, times(1)).discover(anyList(), any(LocalDateTime.class));
    }

    @Test
    void shouldSettleSingleLostBetFromScraperData() {
        LocalDateTime placed = LocalDateTime.of(2026, 5, 1, 12, 0);
        Bet bet = Bet.builder()
                .id(4L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("Lech Poznan")
                .eventName("Legia Warszawa vs Lech Poznan")
                .stake(new BigDecimal("10")).odds(new BigDecimal("3.00"))
                .potentialWinnings(new BigDecimal("30")).placedAt(placed)
                .build();

        when(betRepository.findPendingRootIds(eq(BetStatus.PENDING), any())).thenReturn(List.of(bet.getId()));
        when(betRepository.findRootsWithLegsByIds(anyList())).thenReturn(List.of(bet));
        when(betRepository.findByIdWithChildBets(4L)).thenReturn(Optional.of(bet));
        when(discoveryService.discover(anyList(), any(LocalDateTime.class)))
                .thenReturn(new DiscoveryResult(List.of(finishedEvent(placed.plusDays(1))), 1, Set.of(4L), 0, 0));

        service.resolvePendingBets();

        assertEquals(BetStatus.LOST, bet.getStatus());
        assertEquals(0, new BigDecimal("-10").compareTo(bet.getFinalProfit()));
        assertEquals("APIFY_SOFASCORE", bet.getResolutionSource());
        assertNotNull(bet.getSettledAt());
        verify(betRepository).save(bet);
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

        when(betRepository.findPendingRootIds(eq(BetStatus.PENDING), any())).thenReturn(List.of(bet.getId()));
        when(betRepository.findRootsWithLegsByIds(anyList())).thenReturn(List.of(bet));
        when(betRepository.findByIdWithChildBets(1L)).thenReturn(Optional.of(bet));
        when(discoveryService.discover(anyList(), any(LocalDateTime.class)))
                .thenReturn(new DiscoveryResult(List.of(finishedEvent(placed.plusDays(1))), 1, Set.of(1L), 0, 0));

        service.resolvePendingBets();

        assertEquals(BetStatus.WON, bet.getStatus());
        assertEquals(0, new BigDecimal("10").compareTo(bet.getFinalProfit()));
        assertEquals("APIFY_SOFASCORE", bet.getResolutionSource());
        assertNotNull(bet.getSettledAt());
        verify(betRepository).save(bet);
        verify(discoveryService).discover(anyList(), any(LocalDateTime.class));
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

        when(betRepository.findPendingRootIds(eq(BetStatus.PENDING), any())).thenReturn(List.of(bet.getId()));
        when(betRepository.findRootsWithLegsByIds(anyList())).thenReturn(List.of(bet));
        when(betRepository.findByIdWithChildBets(2L)).thenReturn(Optional.of(bet));
        when(discoveryService.discover(anyList(), any(LocalDateTime.class)))
                .thenReturn(new DiscoveryResult(List.of(), 1, Set.of(2L), 0, 0));

        service.resolvePendingBets();

        assertEquals(BetStatus.PENDING, bet.getStatus());
        assertNull(bet.getLastResolutionAttemptAt(), "pusta pula Apify nie powinna ustawiać cooldownu");
        verify(betRepository).save(bet);
    }

    @Test
    void shouldSkipApifyWhenCooldownActive() {
        Bet bet = Bet.builder()
                .id(3L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("Legia")
                .eventName("Legia Warszawa vs Lech Poznan")
                .placedAt(LocalDateTime.now().minusDays(2))
                .lastResolutionAttemptAt(LocalDateTime.now().minusHours(1))
                .build();

        when(betRepository.findPendingRootIds(eq(BetStatus.PENDING), any())).thenReturn(List.of(bet.getId()));
        when(betRepository.findRootsWithLegsByIds(anyList())).thenReturn(List.of(bet));

        service.resolvePendingBets();

        verify(discoveryService).discover(eq(List.of()), any(LocalDateTime.class));
        verify(betRepository, never()).save(any());
    }

    @Test
    void shouldInvokeDiscoveryForEligibleBets() {
        LocalDateTime placed = LocalDateTime.of(2026, 5, 1, 12, 0);
        Bet b1 = searchableSingle(10L, "Legia Warszawa - Lech Poznan", placed);
        Bet b2 = searchableSingle(11L, "Brighton - Manchester United", placed);
        Bet b3 = searchableSingle(12L, "Germany - Norway", placed);

        when(betRepository.findPendingRootIds(eq(BetStatus.PENDING), any()))
                .thenReturn(List.of(10L, 11L, 12L));
        when(betRepository.findRootsWithLegsByIds(anyList())).thenReturn(List.of(b1, b2, b3));
        when(betRepository.findByIdWithChildBets(10L)).thenReturn(Optional.of(b1));
        when(betRepository.findByIdWithChildBets(11L)).thenReturn(Optional.of(b2));
        when(betRepository.findByIdWithChildBets(12L)).thenReturn(Optional.of(b3));
        when(discoveryService.discover(anyList(), any(LocalDateTime.class)))
                .thenReturn(new DiscoveryResult(List.of(), 2, Set.of(10L, 11L, 12L), 0, 0));

        runResolutionForced(true);

        verify(discoveryService).discover(argThat(list -> list.size() == 3), any(LocalDateTime.class));
    }

    @Test
    void shouldSkipBetsWhenDiscoveryReturnsNoFetchedBetIds() {
        LocalDateTime placed = LocalDateTime.of(2026, 5, 1, 12, 0);
        Bet b1 = searchableSingle(10L, "Legia Warszawa - Lech Poznan", placed);
        Bet b2 = searchableSingle(11L, "Brighton - Manchester United", placed);
        Bet b3 = searchableSingle(12L, "Germany - Norway", placed);

        when(betRepository.findPendingRootIds(eq(BetStatus.PENDING), any()))
                .thenReturn(List.of(10L, 11L, 12L));
        when(betRepository.findRootsWithLegsByIds(anyList())).thenReturn(List.of(b1, b2, b3));
        when(betRepository.findByIdWithChildBets(10L)).thenReturn(Optional.of(b1));
        when(betRepository.findByIdWithChildBets(11L)).thenReturn(Optional.of(b2));
        when(betRepository.findByIdWithChildBets(12L)).thenReturn(Optional.of(b3));
        when(discoveryService.discover(anyList(), any(LocalDateTime.class)))
                .thenReturn(DiscoveryResult.empty());

        runResolutionForced(true);

        assertNull(b1.getLastResolutionAttemptAt());
        assertNull(b2.getLastResolutionAttemptAt());
        assertNull(b3.getLastResolutionAttemptAt());
    }

    @Test
    void shouldResolveFromCachedDiscoveryResultOnSecondCycle() {
        LocalDateTime placed = LocalDateTime.of(2026, 5, 1, 12, 0);
        Bet bet = Bet.builder()
                .id(20L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2).selection("Legia Warszawa")
                .eventName("Legia Warszawa - Lech Poznan")
                .stake(new BigDecimal("10")).odds(new BigDecimal("2.00"))
                .potentialWinnings(new BigDecimal("20")).placedAt(placed)
                .build();
        SofaScoreEventDto event = finishedEvent(placed.plusDays(1));

        when(betRepository.findPendingRootIds(eq(BetStatus.PENDING), any())).thenReturn(List.of(bet.getId()));
        when(betRepository.findRootsWithLegsByIds(anyList())).thenReturn(List.of(bet));
        when(betRepository.findByIdWithChildBets(20L)).thenReturn(Optional.of(bet));
        when(discoveryService.discover(anyList(), any(LocalDateTime.class)))
                .thenReturn(new DiscoveryResult(List.of(event), 0, Set.of(20L), 0, 1));

        runResolutionForced(true);

        assertEquals(BetStatus.WON, bet.getStatus());
        verify(discoveryService, times(1)).discover(anyList(), any(LocalDateTime.class));
    }

    @Test
    void shouldRejectUnparseableBetBuilderFromAutoResolution() {
        Bet bet = Bet.builder()
                .id(50L).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.TOTALS_OVER_UNDER)
                .selection("BetBuilder: niestandardowy opis bez rozpoznawalnych rynków")
                .eventName("Maroko - Norwegia")
                .placedAt(LocalDateTime.of(2026, 6, 1, 12, 0))
                .build();

        when(betRepository.findPendingRootIds(eq(BetStatus.PENDING), any())).thenReturn(List.of(bet.getId()));
        when(betRepository.findRootsWithLegsByIds(anyList())).thenReturn(List.of(bet));

        service.resolvePendingBets();

        verify(discoveryService).discover(eq(List.of()), any(LocalDateTime.class));
        assertNull(bet.getLastResolutionAttemptAt());
    }

    private Bet searchableSingle(Long id, String eventName, LocalDateTime placed) {
        return Bet.builder()
                .id(id).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .selection("1")
                .eventName(eventName)
                .stake(new BigDecimal("10")).odds(new BigDecimal("2.00"))
                .placedAt(placed)
                .build();
    }
}
