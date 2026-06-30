package com.grzechuhehe.SportsBettingManagerApp.config;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.BetResolutionEligibilityEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class ResolutionBlockingReasonBackfillRunner implements ApplicationRunner {

    private static final int BATCH_SIZE = 200;

    private final BetRepository betRepository;
    private final BetResolutionEligibilityEvaluator eligibilityEvaluator;

    @Value("${bet.resolution.blocking-reason-backfill-on-startup:true}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int page = 0;
        int updated = 0;
        List<Long> batchIds;
        do {
            batchIds = betRepository.findPendingNonRetroactiveLeafIds(
                    BetStatus.PENDING, PageRequest.of(page++, BATCH_SIZE));
            if (batchIds.isEmpty()) {
                break;
            }
            List<Bet> legs = new ArrayList<>();
            betRepository.findAllById(batchIds).forEach(legs::add);
            legs.forEach(leg -> eligibilityEvaluator.isEligible(leg, now, false));
            betRepository.saveAll(legs);
            updated += legs.size();
        } while (batchIds.size() == BATCH_SIZE);
        log.info("Resolution blocking reason backfill: updated {} pending legs", updated);
    }
}
