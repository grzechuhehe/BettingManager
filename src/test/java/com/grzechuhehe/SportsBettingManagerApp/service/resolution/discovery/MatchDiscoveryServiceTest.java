package com.grzechuhehe.SportsBettingManagerApp.service.resolution.discovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchDiscoveryServiceTest {

    private MatchDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        discoveryService = new MatchDiscoveryService(null, null, null, null);
    }

    @Test
    void discoverWithEmptyEligibleListReturnsEmptyResult() {
        LocalDateTime now = LocalDateTime.now();

        DiscoveryResult result = discoveryService.discover(List.of(), now);

        assertNotNull(result);
        assertTrue(result.events().isEmpty());
        assertEquals(0, result.apifyCalls());
        assertTrue(result.fetchedBetIds().isEmpty());
        assertEquals(0, result.apifyFailures());
        assertEquals(0, result.cacheHits());
    }
}
