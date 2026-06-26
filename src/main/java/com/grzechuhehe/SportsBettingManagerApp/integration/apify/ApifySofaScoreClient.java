package com.grzechuhehe.SportsBettingManagerApp.integration.apify;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
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
            @Value("${apify.sofascore.use-apify-proxy:false}") boolean useApifyProxy,
            @Value("${apify.api.connect-timeout-ms:15000}") long connectTimeoutMs,
            @Value("${apify.api.read-timeout-ms:300000}") long readTimeoutMs) {
        this.token = token;
        this.actor = actor;
        this.useApifyProxy = useApifyProxy;
        // run-sync actora trwa minuty — bez read-timeout zawieszone połączenie
        // zablokowałoby jednowątkowy scheduler na czas nieokreślony.
        // Wartości <= 0 pomijają own factory (np. w testach z MockRestServiceServer
        // podpiętym pod request factory buildera).
        if (connectTimeoutMs > 0 || readTimeoutMs > 0) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            if (connectTimeoutMs > 0) {
                requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
            }
            if (readTimeoutMs > 0) {
                requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
            }
            restClientBuilder = restClientBuilder.requestFactory(requestFactory);
        }
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        if (token == null || token.isBlank()) {
            logger.warn("Brak tokenu Apify — Apify zwróci 402. Dodaj APIFY_API_TOKEN=apify_api_... do .env i uruchom: docker compose up -d --force-recreate backend");
        } else if (token.equals("dummy-apify-token")) {
            logger.warn("Token Apify nie ustawiony (fallback) — Apify zwróci 401. Dodaj APIFY_API_TOKEN=apify_api_... do .env i uruchom: docker compose up -d --force-recreate backend");
        }
        logger.info("Apify SofaScore proxy: useApifyProxy={}", useApifyProxy);
    }

    public List<SofaScoreEventDto> searchMatches(String query) {
        ApifyBatchResult result = searchMatchesBatch(List.of(query));
        return result.successful() ? result.matches() : List.of();
    }

    public ApifyBatchResult searchMatchesBatch(List<String> queries) {
        List<String> normalized = normalizeQueries(queries);
        if (normalized.isEmpty()) {
            return ApifyBatchResult.success(List.of());
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
        ApifyBatchResult result = runActor(body, "scheduled " + startDate + " +" + daysAhead + "d");
        return result.successful() ? result.matches() : List.of();
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

    private ApifyBatchResult runActor(Map<String, Object> body, String label) {
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
                    return ApifyBatchResult.success(List.of());
                }
                List<SofaScoreEventDto> matches = items.stream()
                        .filter(e -> "match".equals(e.getType()))
                        .toList();
                logger.info("Apify {}: {} rekordów match z {}", label, matches.size(), items.size());
                return ApifyBatchResult.success(matches);
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
        return ApifyBatchResult.failure();
    }

    private static boolean isRetryable(Exception e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        return msg.contains("408")
                || msg.contains("502")
                || msg.contains("503")
                || msg.contains("504")
                || msg.contains("429")
                || msg.contains("run-timeout-exceeded");
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
