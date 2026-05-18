package com.grzechuhehe.SportsBettingManagerApp.integration.polymarket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolymarketApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PolymarketApiClient client;

    @Test
    void shouldCalculateProbabilityFromGammaApi() {
        String mockResponse = "[{\"markets\": [{\"outcomePrices\": [\"0.65\"]}]}]";
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        BigDecimal prob = client.fetchTrueProbability("Real Madrid");
        
        assertNotNull(prob);
        assertEquals(new BigDecimal("0.6500"), prob);
    }
}
