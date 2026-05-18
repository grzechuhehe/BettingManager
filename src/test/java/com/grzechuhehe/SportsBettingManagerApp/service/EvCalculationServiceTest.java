package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.dto.EvCalculationRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.EvCalculationResponse;
import com.grzechuhehe.SportsBettingManagerApp.integration.polymarket.PolymarketApiClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvCalculationServiceTest {

    @Test
    void calculateExpectedValue_ShouldReturnCorrectEV() {
        PolymarketApiClient mockClient = Mockito.mock(PolymarketApiClient.class);
        Mockito.when(mockClient.fetchTrueProbability("Lakers")).thenReturn(new BigDecimal("0.55")); // 55% true prob
        
        EvCalculationService service = new EvCalculationService(mockClient);
        EvCalculationRequest req = new EvCalculationRequest("Lakers", new BigDecimal("2.00")); // Odds 2.0
        
        EvCalculationResponse res = service.calculateExpectedValue(req);
        
        // EV = (2.0 * 0.55) - 1 = 0.10 (+10%)
        assertEquals(new BigDecimal("10.00"), res.getExpectedValuePercentage());
        assertTrue(res.isPositiveEv());
    }
}
