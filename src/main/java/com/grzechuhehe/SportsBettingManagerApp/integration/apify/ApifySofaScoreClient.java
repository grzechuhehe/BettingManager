package com.grzechuhehe.SportsBettingManagerApp.integration.apify;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class ApifySofaScoreClient {
    private static final Logger logger = LoggerFactory.getLogger(ApifySofaScoreClient.class);

    private final RestClient restClient;
    private final String token;
    private final String actor;

    public ApifySofaScoreClient(
            RestClient.Builder restClientBuilder,
            @Value("${apify.api.base-url:https://api.apify.com/v2}") String baseUrl,
            @Value("${apify.api.token:dummy-apify-token}") String token,
            @Value("${apify.actor.sofascore:abotapi~sofascore-scraper}") String actor) {
        this.token = token;
        this.actor = actor;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * Wyszukuje mecze w SofaScore (przez Apify) pasujące do zapytania.
     * Zwraca pustą listę przy dowolnym błędzie (zakład zostanie wtedy PENDING).
     */
    public List<SofaScoreEventDto> searchMatches(String query) {
        logger.info("Apify SofaScore search dla zapytania: {}", query);
        Map<String, Object> body = Map.of(
                "mode", "search",
                "searchQueries", List.of(query),
                "searchType", "match",
                "maxItems", 10,
                "includeStatistics", false,
                "includeLineups", false,
                "includeIncidents", false,
                "includeStandings", false,
                "proxy", Map.of("useApifyProxy", true)
        );
        try {
            List<SofaScoreEventDto> items = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/acts/{actor}/run-sync-get-dataset-items")
                            .queryParam("token", token)
                            .build(actor))
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<SofaScoreEventDto>>() {});
            if (items == null) {
                return List.of();
            }
            return items.stream()
                    .filter(e -> "match".equals(e.getType()))
                    .toList();
        } catch (Exception e) {
            logger.error("Błąd Apify SofaScore dla zapytania '{}': {}", query, e.getMessage());
            return List.of();
        }
    }
}
