package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetResolutionAttemptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolutionHealthMonitorTest {

    @Test
    void shouldWarnWhenSuccessLowAndBacklogHigh() {
        ResolutionHealthMonitor monitor = new ResolutionHealthMonitor(
                stubAttemptRepo(2L), stubBetRepo(79L), 3, 50);

        Optional<ResolutionHealthAlert> alert = monitor.evaluate(LocalDateTime.now());

        assertTrue(alert.isPresent());
        assertEquals(ResolutionHealthAlert.Level.WARN, alert.get().level());
    }

    private BetResolutionAttemptRepository stubAttemptRepo(long count) {
        BetResolutionAttemptRepository repo = mock(BetResolutionAttemptRepository.class);
        when(repo.countSuccessSince(any())).thenReturn(count);
        return repo;
    }

    private BetRepository stubBetRepo(long count) {
        BetRepository repo = mock(BetRepository.class);
        when(repo.countPendingNonRetroactiveLeaves(BetStatus.PENDING)).thenReturn(count);
        return repo;
    }
}
