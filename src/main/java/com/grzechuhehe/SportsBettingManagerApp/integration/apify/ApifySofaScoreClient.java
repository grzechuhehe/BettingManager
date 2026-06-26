package com.grzechuhehe.SportsBettingManagerApp.integration.apify;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ApifySofaScoreClient {
    private static final Logger logger = LoggerFactory.getLogger(ApifySofaScoreClient.class);
    private static final ParameterizedTypeReference<List<SofaScoreEventDto>> MATCH_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String token;
    private final String actor;
    private final boolean useApifyProxy;

    public ApifySofaScoreClient(
            RestClient.Builder restClientBuilder,
            @Value("${apify.api.base-url:https://api.apify.com/v2}") String baseUrl,
            @Value("${apify.api.token:dummy-apify-token}") String token,
            @Value("${apify.actor.sofascore:abotapi~sofascore-scraper}") String actor,
            @Value("${apify.sofascore.use-apify-proxy:false}") boolean useApifyProxy) {
        this.token = token;
        this.actor = actor;
        this.useApifyProxy = useApifyProxy;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        if (token == null || token.isBlank()) {
            logger.warn("Brak tokenu Apify — Apify zwróci 402. Dodaj APIFY_API_TOKEN=apify_api_... do .env i uruchom: docker compose up -d --force-recreate backend");
        } else if (token.equals("dummy-apify-token")) {
            logger.warn("Token Apify nie ustawiony (fallback) — Apify zwróci 401. Dodaj APIFY_API_TOKEN=apify_api_... do .env i uruchom: docker compose up -d --force-recreate backend");
        }
        logger.info("Apify SofaScore proxy: useApifyProxy={}", useApifyProxy);
    }

    public List<SofaScoreEventDto> searchMatches(String query) {
        return searchMatchesBatch(List.of(query));
    }

    public List<SofaScoreEventDto> searchMatchesBatch(List<String> queries) {
        List<String> normalized = normalizeQueries(queries);
        if (normalized.isEmpty()) {
            return List.of();
        }
        logger.info("Apify SofaScore batch search: {} zapytań", normalized.size());
        Map<String, Object> body = baseBody();
        body.put("mode", "search");
        body.put("searchQueries", normalized);
        body.put("searchType", "match");
        body.put("maxItems", Math.min(normalized.size() * 10, 100));
        return runActor(body, "batch search (" + normalized.size() + " zapytań)");
    }

    public List<SofaScoreEventDto> fetchScheduledMatches(
            LocalDate startDate,
            int daysAhead,
            List<String> sports,
            int maxItems) {
        List<String> sportList = sports == null || sports.isEmpty() ? List.of("football") : sports;
        logger.info("Apify SofaScore scheduled: date={}, daysAhead={}, sports={}, proxy={}",
                startDate, daysAhead, sportList, useApifyProxy);
        Map<String, Object> body = baseBody();
        body.put("mode", "scheduled");
        body.put("date", startDate.toString());
        body.put("daysAhead", Math.max(daysAhead, 0));
        body.put("sports", sportList);
        body.put("maxItems", maxItems);
        return runActor(body, "scheduled " + startDate + " +" + daysAhead + "d");
    }

    private Map<String, Object> baseBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("includeStatistics", false);
        body.put("includeLineups", false);
        body.put("includeIncidents", false);
        body.put("includeStandings", false);
        body.put("proxy", Map.of("useApifyProxy", useApifyProxy));
        return body;
    }

    private List<SofaScoreEventDto> runActor(Map<String, Object> body, String label) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                List<SofaScoreEventDto> items = restClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/acts/{actor}/run-sync-get-dataset-items")
                                .queryParam("token", token)
                                .build(actor))
                        .body(body)
                        .retrieve()
                        .body(MATCH_LIST);
                if (items == null) {
                    return List.of();
                }
                List<SofaScoreEventDto> matches = items.stream()
                        .filter(e -> "match".equals(e.getType()))
                        .toList();
                logger.info("Apify {}: {} rekordów match z {}", label, matches.size(), items.size());
                return matches;
            } catch (Exception e) {
                lastError = e;
                if (attempt == 1 && isRetryable(e)) {
                    logger.warn("Apify {}: {} — ponawiam za 3s", label, e.getMessage());
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        logger.error("Błąd Apify SofaScore ({}): {}", label, lastError != null ? lastError.getMessage() : "unknown");
        return List.of();
    }

    private static boolean isRetryable(Exception e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        return msg.contains("502") || msg.contains("503") || msg.contains("504") || msg.contains("429");
    }

    private static List<String> normalizeQueries(List<String> queries) {
        Set<String> unique = new LinkedHashSet<>();
        for (String query : queries) {
            if (query != null && !query.isBlank()) {
                unique.add(query.trim());
            }
        }
        return new ArrayList<>(unique);
    }
}
