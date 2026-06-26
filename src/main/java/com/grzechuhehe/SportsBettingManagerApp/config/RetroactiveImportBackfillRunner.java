package com.grzechuhehe.SportsBettingManagerApp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Idempotentny backfill flag retroactive/pre-match na istniejących wierszach
 * (projekt używa ddl-auto=update zamiast Flyway).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetroactiveImportBackfillRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int markedRetroactive = jdbcTemplate.update("""
                UPDATE bet
                SET retroactive_at_import = 1
                WHERE is_ai_extracted = 1
                  AND status IN ('WON', 'LOST', 'VOID', 'HALF_WON', 'HALF_LOST', 'CASHED_OUT')
                  AND (resolution_source IS NULL OR resolution_source != 'APIFY_SOFASCORE')
                  AND retroactive_at_import = 0
                """);

        int apifyPreMatch = jdbcTemplate.update("""
                UPDATE bet
                SET is_pre_match = 1
                WHERE resolution_source = 'APIFY_SOFASCORE'
                  AND is_pre_match = 0
                """);

        int clearedRetroactive = jdbcTemplate.update("""
                UPDATE bet
                SET retroactive_at_import = 0
                WHERE (resolution_source = 'APIFY_SOFASCORE' OR status = 'PENDING')
                  AND retroactive_at_import = 1
                """);

        if (markedRetroactive + apifyPreMatch + clearedRetroactive > 0) {
            log.info(
                    "Retroactive import backfill: markedRetroactive={}, apifyPreMatch={}, clearedRetroactive={}",
                    markedRetroactive, apifyPreMatch, clearedRetroactive);
        }
    }
}
