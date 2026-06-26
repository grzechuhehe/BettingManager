package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.SofaScoreQueryCache;
import com.grzechuhehe.SportsBettingManagerApp.repository.SofaScoreQueryCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SofaScoreCacheService {

    private static final TypeReference<List<SofaScoreEventDto>> EVENT_LIST_TYPE = new TypeReference<>() {};

    private final SofaScoreQueryCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;

    @Value("${bet.resolution.cache-ttl-hours:72}")
    private int cacheTtlHours;

    public record CacheLookupResult(List<SofaScoreEventDto> events, Set<String> missingQueries) {}

    public CacheLookupResult getFresh(List<String> queries, LocalDateTime now) {
        List<SofaScoreEventDto> events = new ArrayList<>();
        Set<String> missing = new LinkedHashSet<>();

        Map<String, String> queryToHash = queries.stream()
                .filter(q -> q != null && !q.isBlank())
                .collect(Collectors.toMap(Function.identity(), q -> sha256(normalize(q)), (a, b) -> a, java.util.LinkedHashMap::new));

        if (queryToHash.isEmpty()) {
            return new CacheLookupResult(List.of(), Set.of());
        }

        Map<String, SofaScoreQueryCache> byHash = cacheRepository
                .findByQueryHashInAndExpiresAtAfter(queryToHash.values(), now)
                .stream()
                .collect(Collectors.toMap(SofaScoreQueryCache::getQueryHash, row -> row, (a, b) -> a));

        for (String query : queryToHash.keySet()) {
            String hash = queryToHash.get(query);
            SofaScoreQueryCache row = byHash.get(hash);
            if (row != null) {
                events.addAll(deserialize(row.getPayloadJson()));
            } else {
                missing.add(query);
            }
        }

        return new CacheLookupResult(events, missing);
    }

    @Transactional
    public void putAll(Map<String, List<SofaScoreEventDto>> byQuery, LocalDateTime now) {
        if (byQuery == null || byQuery.isEmpty()) {
            return;
        }
        LocalDateTime expiresAt = now.plusHours(cacheTtlHours);
        for (Map.Entry<String, List<SofaScoreEventDto>> entry : byQuery.entrySet()) {
            String query = entry.getKey();
            if (query == null || query.isBlank()) {
                continue;
            }
            List<SofaScoreEventDto> events = entry.getValue() == null ? List.of() : entry.getValue();
            String hash = sha256(normalize(query));

            SofaScoreQueryCache row = cacheRepository.findByQueryHash(hash).orElseGet(SofaScoreQueryCache::new);
            row.setQueryHash(hash);
            row.setQueryText(query);
            row.setPayloadJson(serialize(events));
            row.setEventCount(events.size());
            row.setFetchedAt(now);
            row.setExpiresAt(expiresAt);
            row.setSource("APIFY");
            cacheRepository.save(row);
        }
    }

    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void purgeExpired() {
        LocalDateTime cutoff = LocalDateTime.now();
        cacheRepository.deleteByExpiresAtBefore(cutoff);
        log.debug("SofaScore query cache: usunięto wpisy wygasłe przed {}", cutoff);
    }

    static String normalize(String query) {
        return query.trim().toLowerCase(Locale.ROOT);
    }

    static String sha256(String normalized) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private List<SofaScoreEventDto> deserialize(String payloadJson) {
        try {
            List<SofaScoreEventDto> events = objectMapper.readValue(payloadJson, EVENT_LIST_TYPE);
            return events == null ? List.of() : events;
        } catch (Exception e) {
            log.warn("SofaScore cache: błąd deserializacji payload — traktuję jako miss: {}", e.getMessage());
            return List.of();
        }
    }

    private String serialize(List<SofaScoreEventDto> events) {
        try {
            return objectMapper.writeValueAsString(events);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize SofaScore events for cache", e);
        }
    }
}
