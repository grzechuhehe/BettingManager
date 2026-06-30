package com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class TennisNameNormalizerTest {

    private final TennisNameNormalizer normalizer = new TennisNameNormalizer();

    @Test
    void shouldExtractSurnamesFromCommaFormat() {
        Set<String> tokens = normalizer.extraTokens("Cobolli, Flavio");
        assertTrue(tokens.contains("cobolli"));
        assertTrue(tokens.contains("flavio"));
    }

    @Test
    void shouldExtractSurnameFromInitialFormat() {
        Set<String> tokens = normalizer.extraTokens("Faria J.");
        assertTrue(tokens.contains("faria"));
    }

    @Test
    void shouldExtractSurnameFromWinnerSuffix() {
        Set<String> tokens = normalizer.extraTokens("O'Connell C. - Winner");
        assertTrue(tokens.contains("oconnell"));
    }
}
