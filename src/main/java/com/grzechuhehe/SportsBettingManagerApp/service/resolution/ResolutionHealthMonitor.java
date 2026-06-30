package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetResolutionAttemptRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class ResolutionHealthMonitor {

    private final BetResolutionAttemptRepository attemptRepository;
    private final BetRepository betRepository;
    private final int minSuccessPer24h;
    private final int pendingThreshold;

    public ResolutionHealthMonitor(
            BetResolutionAttemptRepository attemptRepository,
            BetRepository betRepository,
            @Value("${bet.resolution.health-min-success-per-24h:3}") int minSuccessPer24h,
            @Value("${bet.resolution.health-pending-threshold:50}") int pendingThreshold) {
        this.attemptRepository = attemptRepository;
        this.betRepository = betRepository;
        this.minSuccessPer24h = minSuccessPer24h;
        this.pendingThreshold = pendingThreshold;
    }

    public Optional<ResolutionHealthAlert> evaluate(LocalDateTime now) {
        long success = attemptRepository.countSuccessSince(now.minusHours(24));
        long pending = betRepository.countPendingNonRetroactiveLeaves(BetStatus.PENDING);
        if (pending > pendingThreshold && success < minSuccessPer24h) {
            String msg = String.format(
                    "Resolution health WARN: SUCCESS=%d/24h < %d przy pending=%d > %d",
                    success, minSuccessPer24h, pending, pendingThreshold);
            log.warn(msg);
            return Optional.of(new ResolutionHealthAlert(
                    ResolutionHealthAlert.Level.WARN, success, pending, msg));
        }
        return Optional.empty();
    }
}
