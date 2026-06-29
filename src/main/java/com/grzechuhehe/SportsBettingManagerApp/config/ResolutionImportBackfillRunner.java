package com.grzechuhehe.SportsBettingManagerApp.config;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.importing.BetImportResolutionEnricher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class ResolutionImportBackfillRunner implements ApplicationRunner {

    private final BetRepository betRepository;
    private final BetImportResolutionEnricher enricher;

    @Value("${bet.resolution.backfill-on-startup:false}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        var roots = betRepository.findByStatusAndParentBetIsNull(BetStatus.PENDING);
        log.info("Resolution import backfill: enriching {} PENDING roots", roots.size());
        roots.forEach(root -> {
            enricher.enrich(root);
            if (root.getChildBets() != null) {
                root.getChildBets().forEach(enricher::enrich);
            }
            betRepository.save(root);
        });
        log.info("Resolution import backfill: done ({} roots)", roots.size());
    }
}
