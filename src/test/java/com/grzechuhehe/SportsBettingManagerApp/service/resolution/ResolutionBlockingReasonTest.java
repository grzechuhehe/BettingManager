package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetResolutionAttemptRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.discovery.MatchDiscoveryService;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.discovery.ResolutionQueuePrioritizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ResolutionBlockingReasonTest {

    @Mock private BetRepository betRepository;
    @Mock private MatchDiscoveryService discoveryService;
    @Mock private BetResolutionAttemptRepository attemptRepository;
    @Mock private ResolutionCycleMetricsHolder metricsHolder;

    private BetResolutionService service;

    @BeforeEach
    void setUp() {
        ResolutionTestFixtures.ResolutionComponents c = ResolutionTestFixtures.components();
        BetResolutionTransactionService resolutionTx = ResolutionTestFixtures.transactionService(betRepository);
        service = new BetResolutionService(
                c.nameTranslator(),
                resolutionTx,
                c.resolvabilityChecker(),
                new AutoResolutionGuard(),
                new ResolutionQueuePrioritizer(),
                discoveryService,
                attemptRepository,
                metricsHolder);
        ReflectionTestUtils.setField(service, "searchCooldownHours", 24);
        ReflectionTestUtils.setField(service, "minHoursAfterPlaced", 3);
    }

    @Test
    void outrightBetGetsOutrightUnsupportedBlockingReason() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        Bet bet = Bet.builder()
                .betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.OUTRIGHT)
                .selection("Brazylia")
                .eventName("Legia Warszawa - Lech Poznan")
                .placedAt(now.minusDays(1))
                .build();

        Boolean eligible = ReflectionTestUtils.invokeMethod(service, "isEligibleLeaf", bet, now, false);

        assertFalse(eligible);
        assertEquals(ResolutionBlockingReason.OUTRIGHT_UNSUPPORTED, bet.getResolutionBlockingReason());
    }

    @Test
    void eligibleBetClearsBlockingReason() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        Bet bet = Bet.builder()
                .betType(BetType.SINGLE).status(BetStatus.PENDING)
                .marketType(MarketType.MONEYLINE_1X2)
                .selection("Legia Warszawa")
                .eventName("Legia Warszawa - Lech Poznan")
                .placedAt(now.minusDays(1))
                .resolutionBlockingReason(ResolutionBlockingReason.COOLDOWN)
                .build();

        Boolean eligible = ReflectionTestUtils.invokeMethod(service, "isEligibleLeaf", bet, now, false);

        assertTrue(eligible);
        assertNull(bet.getResolutionBlockingReason());
    }
}
