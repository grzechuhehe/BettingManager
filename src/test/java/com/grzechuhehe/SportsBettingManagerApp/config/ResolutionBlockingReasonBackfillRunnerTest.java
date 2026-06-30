package com.grzechuhehe.SportsBettingManagerApp.config;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.BetResolutionEligibilityEvaluator;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionBlockingReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolutionBlockingReasonBackfillRunnerTest {

    @Mock
    private BetRepository betRepository;

    @Mock
    private BetResolutionEligibilityEvaluator eligibilityEvaluator;

    private ResolutionBlockingReasonBackfillRunner runner;

    @BeforeEach
    void setUp() {
        runner = new ResolutionBlockingReasonBackfillRunner(betRepository, eligibilityEvaluator);
        ReflectionTestUtils.setField(runner, "enabled", true);
    }

    @Test
    void shouldPersistBlockingReasonForOutrightLeg() {
        Bet leg = Bet.builder()
                .id(99L).status(BetStatus.PENDING)
                .marketType(MarketType.OUTRIGHT)
                .eventName("MŚ 2026")
                .selection("Argentyna awansuje")
                .placedAt(LocalDateTime.now().minusDays(2))
                .retroactiveAtImport(false)
                .build();
        when(betRepository.findPendingNonRetroactiveLeafIds(eq(BetStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(99L))
                .thenReturn(List.of());
        when(betRepository.findAllById(List.of(99L))).thenReturn(List.of(leg));
        doAnswer(invocation -> {
            Bet bet = invocation.getArgument(0);
            bet.setResolutionBlockingReason(ResolutionBlockingReason.OUTRIGHT_UNSUPPORTED);
            return false;
        }).when(eligibilityEvaluator).isEligible(any(), any(), eq(false));

        runner.run(null);

        verify(betRepository).saveAll(argThat(bets -> {
            Bet saved = bets.iterator().next();
            return saved.getResolutionBlockingReason() == ResolutionBlockingReason.OUTRIGHT_UNSUPPORTED;
        }));
    }
}
