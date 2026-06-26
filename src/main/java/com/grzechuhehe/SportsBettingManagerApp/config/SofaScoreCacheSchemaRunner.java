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
 * Idempotentnie podnosi typ kolumny sofascore_query_cache.payload_json do MEDIUMTEXT.
 * Hibernate (ddl-auto=update) tworzy kolumnę z @Lob jako zbyt mały typ i nie zmienia
 * typu istniejącej kolumny, więc payload większych batchy powodował "Data too long".
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SofaScoreCacheSchemaRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<String> dataType = jdbcTemplate.queryForList("""
                SELECT DATA_TYPE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'sofascore_query_cache'
                  AND COLUMN_NAME = 'payload_json'
                """, String.class);

        if (dataType.isEmpty()) {
            return;
        }

        String current = dataType.get(0) == null ? "" : dataType.get(0).toLowerCase(Locale.ROOT);
        if (current.equals("mediumtext") || current.equals("longtext")) {
            return;
        }

        jdbcTemplate.execute(
                "ALTER TABLE sofascore_query_cache MODIFY payload_json MEDIUMTEXT NOT NULL");
        log.info("SofaScore cache: payload_json zmieniono z {} na MEDIUMTEXT", current);
    }
}
