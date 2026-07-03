package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetResolutionAttemptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BetResolutionTransactionServiceOldestQueueTest {

    @Mock private BetRepository betRepository;
    @Mock private BetResolutionAttemptRepository attemptRepository;
    @Mock private BetMatcher betMatcher;
    @Mock private BetOutcomeEvaluator betOutcomeEvaluator;
    @Mock private ResolutionNameTranslator nameTranslator;
    @Mock private SelectionResolvabilityChecker selectionResolvabilityChecker;
    @Mock private com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.SportConfidenceThresholds sportConfidenceThresholds;
    @Mock private EventEnrichmentService enrichmentService;

    @InjectMocks
    private BetResolutionTransactionService service;

    @Test
    void loadPendingRoots_usesOldestBeforeCutoffQuery() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 6, 25, 0, 0);
        ResolutionRunConfig config = ResolutionRunConfig.oldestBefore(cutoff, 80, 30);
        when(betRepository.findPendingRootIdsBeforeCutoff(eq(BetStatus.PENDING), eq(cutoff), eq(PageRequest.of(0, 80))))
                .thenReturn(List.of(42L));
        when(betRepository.findRootsWithLegsByIds(List.of(42L))).thenReturn(List.of());

        service.loadPendingRoots(config);

        verify(betRepository).findPendingRootIdsBeforeCutoff(BetStatus.PENDING, cutoff, PageRequest.of(0, 80));
    }
}
