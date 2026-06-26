package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.ApifyBatchResult;
import com.grzechuhehe.SportsBettingManagerApp.integration.apify.ApifySofaScoreClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.SofaScoreQueryCache;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.SofaScoreQueryCacheRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetResolutionServiceTest {

    @Mock private BetRepository betRepository;
    @Mock private ApifySofaScoreClient apifySofaScoreClient;
    @Mock private SofaScoreQueryCacheRepository cacheRepository;

    private BetResolutionService service;
    private BetResolutionTransactionService resolutionTx;
    private SofaScoreCacheService cacheService;
    private Map<String, SofaScoreQueryCache> cacheStore;

    @BeforeEach
    void setUp() {
        cacheStore = new HashMap<>();
        ResolutionTestFixtures.ResolutionComponents c = ResolutionTestFixtures.components();
        resolutionTx = ResolutionTestFixtures.transactionService(betRepository);
        cacheService = new SofaScoreCacheService(cacheRepository, new ObjectMapper());
        ReflectionTestUtils.setField(cacheService, "cacheTtlHours", 72);
        stubCacheRepository();

        service = new BetResolutionService(
                apifySofaScoreClient,
                new SofaScoreSportMapper(),
                c.nameTranslator(),
                resolutionTx,
                c.resolvabilityChecker(),
                cacheService);
        ReflectionTestUtils.setField(service, "confidenceThreshold", 0.85);
        ReflectionTestUtils.setField(service, "dateWindowDays", 4);
        ReflectionTestUtils.setField(service, "maxBetsPerRun", 50);
        ReflectionTestUtils.setField(service, "apifyMode", "scheduled");
        ReflectionTestUtils.setField(service, "scheduledSports", "football");
        ReflectionTestUtils.setField(service, "scheduledMaxItems", 400);
        ReflectionTestUtils.setField(service, "searchBatchSize", 8);
        ReflectionTestUtils.setField(service, "maxSearchQueries", 20);
        ReflectionTestUtils.setField(service, "maxApifyCallsPerCycle", 5);
        ReflectionTestUtils.setField(service, "searchCooldownHours", 24);
        ReflectionTestUtils.setField(service, "minHoursAfterPlaced", 3);
        ReflectionTestUtils.setField(service, "scheduledMaxDaysBack", 7);
    }

    private void stubCacheRepository() {
        lenient().when(cacheRepository.findByQueryHashInAndExpiresAtAfter(anyCollection(), any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    Collection<String> hashes = invocation.getArgument(0);
                    LocalDateTime now = invocation.getArgument(1);
                    return cacheStore.values().stream()
                            .filter(row -> hashes.contains(row.getQueryHash()))
                            .filter(row -> row.getExpiresAt().isAfter(now))
                            .toList();
                });
        lenient().when(cacheRepository.findByQueryHash(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(cacheStore.get(invocation.getArgument(0))));
        lenient().when(cacheRepository.save(any(SofaScoreQueryCache.class)))
                .thenAnswer(invocation -> {
                    SofaScoreQueryCache row = invocation.getArgument(0);
                    cacheStore.put(row.getQueryHash(), row);
                    return row;
                });
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
        when(apifySofaScoreClient.fetchScheduledMatches(any(), anyInt(), anyList(), anyInt()))
                .thenReturn(List.of(finishedEvent(placed.plusDays(1))));

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
        when(apifySofaScoreClient.fetchScheduledMatches(any(), anyInt(), anyList(), anyInt()))
                .thenReturn(List.of(finishedEvent(placed.plusDays(1))));

        service.resolvePendingBets();

        assertEquals(BetStatus.WON, bet.getStatus());
        assertEquals(0, new BigDecimal("10").compareTo(bet.getFinalProfit()));
        assertEquals("APIFY_SOFASCORE", bet.getResolutionSource());
        assertNotNull(bet.getSettledAt());
        verify(betRepository).save(bet);
        verify(apifySofaScoreClient).fetchScheduledMatches(any(), anyInt(), anyList(), anyInt());
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
        when(apifySofaScoreClient.fetchScheduledMatches(any(), anyInt(), anyList(), anyInt()))
                .thenReturn(List.of());

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

        verify(apifySofaScoreClient, never()).fetchScheduledMatches(any(), anyInt(), anyList(), anyInt());
        verify(apifySofaScoreClient, never()).searchMatchesBatch(anyList());
        verify(betRepository, never()).save(any());
    }

    @Test
    void shouldFetchAllUniqueQueriesInMultipleApifyBatches() {
        ReflectionTestUtils.setField(service, "apifyMode", "search");
        ReflectionTestUtils.setField(service, "searchBatchSize", 2);

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
        when(apifySofaScoreClient.searchMatchesBatch(anyList()))
                .thenReturn(ApifyBatchResult.success(List.of()));

        service.resolvePendingBets(true);

        verify(apifySofaScoreClient, atLeast(1)).searchMatchesBatch(anyList());
        verify(apifySofaScoreClient, never()).fetchScheduledMatches(any(), anyInt(), anyList(), anyInt());
    }

    @Test
    void shouldSkipBetsWhenApifyCallLimitReached() {
        ReflectionTestUtils.setField(service, "apifyMode", "search");
        ReflectionTestUtils.setField(service, "maxApifyCallsPerCycle", 0);

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

        service.resolvePendingBets(true);

        verify(apifySofaScoreClient, never()).searchMatchesBatch(anyList());
        assertNull(b1.getLastResolutionAttemptAt());
        assertNull(b2.getLastResolutionAttemptAt());
        assertNull(b3.getLastResolutionAttemptAt());
    }

    @Test
    void shouldSkipApifyWhenQueriesCachedWithinTtl() {
        ReflectionTestUtils.setField(service, "apifyMode", "search");

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
        when(apifySofaScoreClient.searchMatchesBatch(anyList()))
                .thenReturn(ApifyBatchResult.success(List.of(event)));

        service.resolvePendingBets(true);
        verify(apifySofaScoreClient, times(1)).searchMatchesBatch(anyList());
        assertFalse(cacheStore.isEmpty());

        reset(apifySofaScoreClient);
        bet.setStatus(BetStatus.PENDING);
        bet.setLastResolutionAttemptAt(null);

        service.resolvePendingBets(true);
        verify(apifySofaScoreClient, never()).searchMatchesBatch(anyList());
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

        verify(apifySofaScoreClient, never()).searchMatchesBatch(anyList());
        verify(apifySofaScoreClient, never()).fetchScheduledMatches(any(), anyInt(), anyList(), anyInt());
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
