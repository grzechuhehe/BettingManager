package com.grzechuhehe.SportsBettingManagerApp.integration.kalshi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.integration.polymarket.MarketData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class KalshiApiClient {
    private final RestTemplate restTemplate;
    private static final String KALSHI_API_URL = "https://api.elections.kalshi.com/trade-api/v2/markets?status=open&limit=1000";
    private static final ObjectMapper mapper = new ObjectMapper();

    public KalshiApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, MarketData> fetchMarketProbabilities(String homeTeam, String awayTeam) {
        Map<String, MarketData> probabilities = new HashMap<>();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(KALSHI_API_URL, String.class);
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode markets = root.path("markets");

            if (markets.isArray()) {
                for (JsonNode market : markets) {
                    String title = market.path("title").asText().toLowerCase();
                    // Better matching logic: check if both teams are in the title
                    if (title.contains(homeTeam.toLowerCase().split(" ")[0]) && 
                        title.contains(awayTeam.toLowerCase().split(" ")[0])) {
                        processMarket(market, probabilities, homeTeam, awayTeam);
                        if (!probabilities.isEmpty()) return probabilities;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error fetching from Kalshi for {} vs {}: {}", homeTeam, awayTeam, e.getMessage());
        }
        return probabilities;
    }

    private void processMarket(JsonNode market, Map<String, MarketData> probabilities, String home, String away) {
        try {
            // Use yes_bid_dollars as primary, last_price as fallback
            String priceStr = market.path("yes_bid_dollars").asText();
            if (priceStr == null || priceStr.equals("0.0000")) {
                priceStr = market.path("last_price_dollars").asText();
            }

            BigDecimal price = new BigDecimal(priceStr);
            BigDecimal openInterest = new BigDecimal(market.path("open_interest_fp").asText());
            String title = market.path("title").asText().toLowerCase();

            if (title.contains(home.toLowerCase().split(" ")[0])) {
                probabilities.put(home, new MarketData(price, openInterest));
            } else if (title.contains(away.toLowerCase().split(" ")[0])) {
                probabilities.put(away, new MarketData(price, openInterest));
            }
        } catch (Exception e) {
            log.debug("Error parsing Kalshi market: {}", e.getMessage());
        }
    }
}
