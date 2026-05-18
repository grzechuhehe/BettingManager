package com.grzechuhehe.SportsBettingManagerApp.integration.polymarket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class PolymarketApiClient {

    private final RestTemplate restTemplate;
    private static final String GAMMA_API_URL = "https://gamma-api.polymarket.com/events?query=";
    private static final ObjectMapper mapper = new ObjectMapper();

    public PolymarketApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public BigDecimal fetchTrueProbability(String eventQuery) {
        try {
            String encodedQuery = URLEncoder.encode(eventQuery, StandardCharsets.UTF_8);
            String url = GAMMA_API_URL + encodedQuery;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
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
            System.err.println("Error fetching from Polymarket: " + e.getMessage());
        }
        // Fallback default probability if not found to avoid breaking EV math
        return new BigDecimal("0.5000"); 
    }
}
