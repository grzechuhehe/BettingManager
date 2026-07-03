package com.grzechuhehe.SportsBettingManagerApp.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileAnalysisOrchestratorTweetDateTest {

    @Test
    void parseTweetCreatedAt_parsesTwitterFormatAsUtc() {
        LocalDateTime actual = ProfileAnalysisOrchestrator.parseTweetCreatedAt(
                "Tue Jun 23 14:30:00 +0000 2026");

        assertThat(actual).isEqualTo(LocalDateTime.of(2026, 6, 23, 14, 30, 0));
    }

    @Test
    void parseTweetCreatedAt_returnsNullForBlank() {
        assertThat(ProfileAnalysisOrchestrator.parseTweetCreatedAt(null)).isNull();
        assertThat(ProfileAnalysisOrchestrator.parseTweetCreatedAt("")).isNull();
    }

    @Test
    void parseTweetCreatedAt_returnsNullForInvalidFormat() {
        assertThat(ProfileAnalysisOrchestrator.parseTweetCreatedAt("not-a-date")).isNull();
    }
}
