package com.grzechuhehe.SportsBettingManagerApp.integration.polymarket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class PolymarketApiClient {

    private final RestTemplate restTemplate;
    private static final String GAMMA_API_URL = "https://gamma-api.polymarket.com/events?query=";
    private static final ObjectMapper mapper = new ObjectMapper();

    public PolymarketApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static final String GAMMA_SEARCH_URL = "https://gamma-api.polymarket.com/public-search?q=";

    public java.util.Map<String, BigDecimal> fetchMarketProbabilities(String homeTeam, String awayTeam) {
        java.util.Map<String, BigDecimal> probabilities = new java.util.HashMap<>();
        String query = homeTeam + " " + awayTeam;
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = GAMMA_SEARCH_URL + encodedQuery;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = mapper.readTree(response.getBody());
            
            // Try events first
            JsonNode events = root.path("events");
            if (events.isArray()) {
                for (JsonNode event : events) {
                    String title = event.path("title").asText().toLowerCase();
                    if (isMatchMatch(title, homeTeam, awayTeam)) {
                        JsonNode markets = event.path("markets");
                        if (markets.isArray() && markets.size() > 0) {
                            processMarket(markets.get(0), probabilities, homeTeam, awayTeam);
                            if (!probabilities.isEmpty()) return probabilities;
                        }
                    }
                }
            }

            // Try markets directly
            JsonNode markets = root.path("markets");
            if (markets.isArray()) {
                for (JsonNode market : markets) {
                    String question = market.path("question").asText().toLowerCase();
                    if (isMatchMatch(question, homeTeam, awayTeam)) {
                        processMarket(market, probabilities, homeTeam, awayTeam);
                        if (!probabilities.isEmpty()) return probabilities;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error fetching probabilities for {} vs {}: {}", homeTeam, awayTeam, e.getMessage());
        }
        return probabilities;
    }

    private boolean isMatchMatch(String text, String home, String away) {
        String homeKey = home.split(" ")[0].toLowerCase();
        String awayKey = away.split(" ")[0].toLowerCase();
        return text.contains(homeKey) && text.contains(awayKey);
    }

    private void processMarket(JsonNode market, java.util.Map<String, BigDecimal> probabilities, String home, String away) {
        try {
            JsonNode outcomesNode = market.path("outcomes");
            JsonNode pricesNode = market.path("outcomePrices");
            
            List<String> outcomes = new java.util.ArrayList<>();
            if (outcomesNode.isTextual()) {
                JsonNode array = mapper.readTree(outcomesNode.asText());
                array.forEach(n -> outcomes.add(n.asText()));
            } else if (outcomesNode.isArray()) {
                outcomesNode.forEach(n -> outcomes.add(n.asText()));
            }

            List<BigDecimal> prices = new java.util.ArrayList<>();
            if (pricesNode.isTextual()) {
                JsonNode array = mapper.readTree(pricesNode.asText());
                array.forEach(n -> prices.add(new BigDecimal(n.asText())));
            } else if (pricesNode.isArray()) {
                pricesNode.forEach(n -> prices.add(new BigDecimal(n.asText())));
            }

            for (int i = 0; i < Math.min(outcomes.size(), prices.size()); i++) {
                String outcomeName = outcomes.get(i);
                BigDecimal price = prices.get(i);
                
                // Map outcome to team
                if (outcomeName.equalsIgnoreCase("Yes") || outcomeName.toLowerCase().contains(home.toLowerCase().split(" ")[0])) {
                    probabilities.put(home, price);
                } else if (outcomeName.equalsIgnoreCase("No") || outcomeName.toLowerCase().contains(away.toLowerCase().split(" ")[0])) {
                    probabilities.put(away, price);
                } else {
                    probabilities.put(outcomeName, price);
                }
            }
        } catch (Exception e) {
            log.warn("Error processing market data: {}", e.getMessage());
        }
    }

    // Keep old method for compatibility or specific queries
    public java.util.Optional<BigDecimal> fetchTrueProbability(String eventQuery) {
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
                        return java.util.Optional.of(new BigDecimal(priceStr).setScale(4, RoundingMode.HALF_UP));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error fetching from Polymarket: {}", e.getMessage());
        }
        return java.util.Optional.empty();
    }
}
