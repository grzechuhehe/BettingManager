package com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DoublesNameNormalizerTest {

    private final DoublesNameNormalizer normalizer = new DoublesNameNormalizer();

    @Test
    void shouldExtractFirstPlayerSurnamesFromTennisDoubles() {
        Optional<String> query = normalizer.toApifyQuery("L.Jurgenson/E.Kuivonen - M.Abdala/A.Ghig");

        assertTrue(query.isPresent());
        assertTrue(query.get().contains("Jurgenson"));
        assertTrue(query.get().contains("Abdala"));
    }

    @Test
    void shouldReturnEmptyForNonDoublesEvent() {
        assertTrue(normalizer.toApifyQuery("Niemcy - Norwegia").isEmpty());
    }

    @Test
    void shouldReturnEmptyForBlankInput() {
        assertTrue(normalizer.toApifyQuery("").isEmpty());
        assertTrue(normalizer.toApifyQuery(null).isEmpty());
    }
}
