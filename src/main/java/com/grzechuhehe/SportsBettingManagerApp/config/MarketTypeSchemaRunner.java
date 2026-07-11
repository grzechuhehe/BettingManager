package com.grzechuhehe.SportsBettingManagerApp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Idempotentnie zmienia bet.market_type z MySQL ENUM na VARCHAR(64).
 * Hibernate (ddl-auto=update) nie rozszerza istniejących kolumn ENUM o nowe wartości
 * z {@link com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType},
 * co powoduje "Data truncated for column 'market_type'" przy imporcie z Gemini.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MarketTypeSchemaRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<String> dataType = jdbcTemplate.queryForList("""
                SELECT DATA_TYPE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'bet'
                  AND COLUMN_NAME = 'market_type'
                """, String.class);

        if (dataType.isEmpty()) {
            return;
        }

        String current = dataType.get(0) == null ? "" : dataType.get(0).toLowerCase(Locale.ROOT);
        if (!current.equals("enum")) {
            return;
        }

        try {
            jdbcTemplate.execute("ALTER TABLE bet MODIFY market_type VARCHAR(64) NULL");
            log.info("bet.market_type zmieniono z ENUM na VARCHAR(64) — obsługa pełnego MarketType z Gemini");
        } catch (Exception e) {
            log.error("Could not widen bet.market_type column (continuing startup): {}", e.getMessage());
        }
    }
}
