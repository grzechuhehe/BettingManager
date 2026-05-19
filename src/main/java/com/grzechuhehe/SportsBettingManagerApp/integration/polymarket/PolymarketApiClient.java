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
            String question = market.path("question").asText().toLowerCase();
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

            String homeLower = home.toLowerCase();
            String awayLower = away.toLowerCase();
            String homeShort = home.split(" ")[0].toLowerCase();
            String awayShort = away.split(" ")[0].toLowerCase();

            for (int i = 0; i < Math.min(outcomes.size(), prices.size()); i++) {
                String outcomeName = outcomes.get(i);
                BigDecimal price = prices.get(i);
                String outcomeLower = outcomeName.toLowerCase();

                // 1. Direct Name Match (Strongest signal)
                if (outcomeLower.contains(homeLower) || (outcomeLower.contains(homeShort) && !outcomeLower.contains(awayShort))) {
                    probabilities.put(home, price);
                } else if (outcomeLower.contains(awayLower) || (outcomeLower.contains(awayShort) && !outcomeLower.contains(homeShort))) {
                    probabilities.put(away, price);
                } else if (outcomeLower.contains("draw")) {
                    probabilities.put("Draw", price);
                } 
                // 2. Binary Market Match (Yes/No)
                else if (outcomeName.equalsIgnoreCase("Yes")) {
                    // Map "Yes" only to the subject of the question
                    if (question.contains(homeLower) || question.contains(homeShort)) {
                        probabilities.put(home, price);
                    } else if (question.contains(awayLower) || question.contains(awayShort)) {
                        probabilities.put(away, price);
                    }
                }
                // Note: We EXPLICITLY do NOT map "No" to the other team for soccer.
                // In soccer, "Will Home win? No" = (Away Win OR Draw).
                // Mapping it directly to Away Win causes massive +EV errors.
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
