package com.grzechuhehe.SportsBettingManagerApp.integration.polymarket;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolymarketApiClientTest {

    @Test
    void shouldCalculateProbabilityFromGammaApi() {
        PolymarketApiClient client = new PolymarketApiClient(new RestTemplate());
        BigDecimal prob = client.fetchTrueProbability("Real Madrid");
        assertNotNull(prob);
        assertTrue(prob.compareTo(BigDecimal.ZERO) >= 0 && prob.compareTo(BigDecimal.ONE) <= 0);
    }
}
