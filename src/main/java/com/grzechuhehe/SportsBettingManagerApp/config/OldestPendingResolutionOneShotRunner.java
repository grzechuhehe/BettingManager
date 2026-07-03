package com.grzechuhehe.SportsBettingManagerApp.config;

import com.grzechuhehe.SportsBettingManagerApp.service.resolution.AutoResolutionGuard;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.BetResolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class OldestPendingResolutionOneShotRunner implements ApplicationRunner {

    private final BetResolutionService betResolutionService;

    @Value("${bet.resolution.oldest-one-shot.enabled:false}")
    private boolean enabled;

    @Value("${bet.resolution.oldest-one-shot.cutoff:2026-06-25T00:00:00}")
    private String cutoff;

    @Value("${bet.resolution.oldest-one-shot.limit:80}")
    private int limit;

    @Value("${bet.resolution.oldest-one-shot.date-window-days:30}")
    private int dateWindowDays;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        LocalDateTime cutoffTime = LocalDateTime.parse(cutoff);
        log.warn("oldest-one-shot ENABLED — starting backlog resolution before {}", cutoffTime);
        AutoResolutionGuard.AcquireResult result =
                betResolutionService.triggerOldestPendingOneShot(cutoffTime, limit, dateWindowDays, true);
        if (result.status() != AutoResolutionGuard.Acquisition.ACQUIRED) {
            log.warn("oldest-one-shot could not acquire guard: {}", result.status());
        }
    }
}
