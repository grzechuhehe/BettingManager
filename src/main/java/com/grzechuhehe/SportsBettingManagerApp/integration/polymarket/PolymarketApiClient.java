package com.grzechuhehe.SportsBettingManagerApp.integration.polymarket;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PolymarketApiClient {

    private final RestTemplate restTemplate;
    private static final String GAMMA_API_URL = "https://gamma-api.polymarket.com/events?query=";

    public PolymarketApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public BigDecimal fetchTrueProbability(String eventQuery) {
        try {
            String url = GAMMA_API_URL + eventQuery.replace(" ", "%20");
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootArray = mapper.readTree(response.getBody());
            
            if (rootArray.isArray() && rootArray.size() > 0) {
                JsonNode firstEvent = rootArray.get(0);
                JsonNode markets = firstEvent.path("markets");
                if (markets.isArray() && markets.size() > 0) {
                    JsonNode outcomePrices = markets.get(0).path("outcomePrices");
                    if (outcomePrices.isArray() && outcomePrices.size() > 0) {
                        String priceStr = outcomePrices.get(0).asText();
                        return new BigDecimal(priceStr).setScale(4, RoundingMode.HALF_UP);
                    }
                }
            }
        } catch (Exception e) {
            // Log warning in real app
        }
        // Fallback default probability if not found to avoid breaking EV math
        return new BigDecimal("0.5000"); 
    }
}
