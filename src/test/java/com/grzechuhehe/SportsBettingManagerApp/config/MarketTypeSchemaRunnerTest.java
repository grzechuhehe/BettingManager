package com.grzechuhehe.SportsBettingManagerApp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketTypeSchemaRunnerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ApplicationArguments applicationArguments;

    private MarketTypeSchemaRunner runner;

    @BeforeEach
    void setUp() {
        runner = new MarketTypeSchemaRunner(jdbcTemplate);
    }

    @Test
    void shouldWidenEnumColumnToVarchar() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of("enum"));

        runner.run(applicationArguments);

        verify(jdbcTemplate).execute("ALTER TABLE bet MODIFY market_type VARCHAR(64) NULL");
    }

    @Test
    void shouldSkipWhenAlreadyVarchar() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of("varchar"));

        runner.run(applicationArguments);

        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void shouldSkipWhenColumnMissing() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(Collections.emptyList());

        runner.run(applicationArguments);

        verify(jdbcTemplate, never()).execute(anyString());
    }
}
