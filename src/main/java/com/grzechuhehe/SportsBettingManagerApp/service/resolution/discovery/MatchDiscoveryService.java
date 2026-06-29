package com.grzechuhehe.SportsBettingManagerApp.service.resolution.discovery;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.ApifySofaScoreClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.SofaScoreCacheService;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.SofaScoreSportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchDiscoveryService {

    private final ApifySofaScoreClient apifySofaScoreClient;
    private final SofaScoreSportMapper sportMapper;
    private final ResolutionNameTranslator nameTranslator;
    private final SofaScoreCacheService sofaScoreCacheService;

    @Value("${bet.resolution.apify-mode:scheduled}")
    private String apifyMode;

    @Value("${bet.resolution.scheduled-sports:football,basketball,tennis,ice-hockey}")
    private String scheduledSports;

    @Value("${bet.resolution.scheduled-max-items:400}")
    private int scheduledMaxItems;

    @Value("${bet.resolution.search-batch-size:8}")
    private int searchBatchSize;

    @Value("${bet.resolution.max-search-queries:20}")
    private int maxSearchQueries;

    @Value("${bet.resolution.max-apify-calls-per-cycle:5}")
    private int maxApifyCallsPerCycle;

    @Value("${bet.resolution.scheduled-max-days-back:7}")
    private int scheduledMaxDaysBack;

    @Value("${bet.resolution.date-window-days:4}")
    private int dateWindowDays;

    public String getApifyMode() {
        return apifyMode;
    }

    public DiscoveryResult discover(List<Bet> eligibleLeaves, LocalDateTime now) {
        if (eligibleLeaves.isEmpty()) {
            return DiscoveryResult.empty();
        }

        if ("search".equalsIgnoreCase(apifyMode)) {
            return fetchBySearchBatch(eligibleLeaves);
        }
        return fetchByScheduled(eligibleLeaves, now);
    }

    private DiscoveryResult fetchByScheduled(List<Bet> eligibleLeaves, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDate earliestAllowed = today.minusDays(scheduledMaxDaysBack);

        LocalDate startFromBets = eligibleLeaves.stream()
                .map(b -> b.getPlacedAt() == null ? today.minusDays(1) : b.getPlacedAt().toLocalDate().minusDays(1))
                .min(LocalDate::compareTo)
                .orElse(today.minusDays(1));
        LocalDate endFromBets = eligibleLeaves.stream()
                .map(b -> b.getPlacedAt() == null ? today : b.getPlacedAt().toLocalDate().plusDays(dateWindowDays))
                .max(LocalDate::compareTo)
                .orElse(today);

        LocalDate start = startFromBets.isBefore(earliestAllowed) ? earliestAllowed : startFromBets;
        LocalDate end = endFromBets.isAfter(today) ? today : endFromBets;
        if (end.isBefore(start)) {
            end = start;
        }
        int daysAhead = (int) Math.min(ChronoUnit.DAYS.between(start, end), 7);

        List<String> fallbackSports = List.of(scheduledSports.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        List<String> sports = sportMapper.resolveSportsForBets(eligibleLeaves, fallbackSports);

        log.info(
                "Scheduled Apify: start={}, daysAhead={}, sports={}, okno z zakładów: {}..{}",
                start, daysAhead, sports, startFromBets, endFromBets
        );

        List<SofaScoreEventDto> events = apifySofaScoreClient.fetchScheduledMatches(
                start, daysAhead, sports, scheduledMaxItems);
        Set<Long> fetchedBetIds = eligibleLeaves.stream().map(Bet::getId).collect(Collectors.toSet());
        return new DiscoveryResult(events, 1, fetchedBetIds, 0, 0);
    }

    /**
     * Batch search: unikalne query w jednym lub kilku wywołaniach actora (~$0.08 każde).
     * Wszystkie zebrane query idą do Apify (w paczkach po searchBatchSize), nie tylko pierwsze N.
     */
    private DiscoveryResult fetchBySearchBatch(List<Bet> eligibleLeaves) {
        LinkedHashSet<String> allQueries = new LinkedHashSet<>();
        Map<String, Set<Long>> queryToBetIds = new LinkedHashMap<>();

        for (Bet bet : eligibleLeaves) {
            String raw = bet.getEventName().trim();
            Optional<String> queryOpt = nameTranslator.resolveQueryForApify(raw);
            if (queryOpt.isEmpty()) {
                continue;
            }
            String query = queryOpt.get();
            queryToBetIds.computeIfAbsent(query, k -> new LinkedHashSet<>()).add(bet.getId());
            if (allQueries.add(query)) {
                log.info("Apify search query (noga/zakład {}): '{}' → '{}'", bet.getId(), raw, query);
            }
        }

        if (allQueries.isEmpty()) {
            log.warn("Apify search: brak zapytań ({} kwalifikujących)", eligibleLeaves.size());
            return DiscoveryResult.empty();
        }

        if (allQueries.size() > maxSearchQueries) {
            log.warn(
                    "Apify search: {} unikalnych query (limit konfiguracyjny {} — nadmiarowe i tak pobieramy w batchach)",
                    allQueries.size(),
                    maxSearchQueries
            );
        }

        LocalDateTime now = LocalDateTime.now();
        SofaScoreCacheService.CacheLookupResult cacheLookup =
                sofaScoreCacheService.getFresh(new ArrayList<>(allQueries), now);

        List<SofaScoreEventDto> all = new ArrayList<>(cacheLookup.events());
        Set<Long> fetchedBetIds = new HashSet<>();
        for (String query : allQueries) {
            if (!cacheLookup.missingQueries().contains(query)) {
                fetchedBetIds.addAll(queryToBetIds.getOrDefault(query, Set.of()));
            }
        }

        int cacheHits = allQueries.size() - cacheLookup.missingQueries().size();
        int calls = 0;
        int apifyFailures = 0;
        List<String> batch = new ArrayList<>(searchBatchSize);

        for (String query : cacheLookup.missingQueries()) {
            batch.add(query);
            if (batch.size() >= searchBatchSize) {
                if (calls >= maxApifyCallsPerCycle) {
                    int processedMissing = calls * searchBatchSize;
                    log.warn(
                            "Apify search: limit {} wywołań/cykl — pominięto ~{} zapytań bez cache",
                            maxApifyCallsPerCycle,
                            Math.max(0, cacheLookup.missingQueries().size() - processedMissing)
                    );
                    break;
                }
                if (appendBatchSearchResult(all, queryToBetIds, fetchedBetIds, batch, now)) {
                    calls++;
                } else {
                    apifyFailures++;
                }
                batch.clear();
            }
        }
        if (!batch.isEmpty() && calls < maxApifyCallsPerCycle) {
            if (appendBatchSearchResult(all, queryToBetIds, fetchedBetIds, batch, now)) {
                calls++;
            } else {
                apifyFailures++;
            }
        }

        List<SofaScoreEventDto> deduped = dedupEventsByUrl(all);

        log.info(
                "Apify batch search: {} unikalnych query, cacheHits={}, wywołań actora={}, apifyFailures={}, {} meczów w puli ({} przed dedup), {} nóg z danymi",
                allQueries.size(),
                cacheHits,
                calls,
                apifyFailures,
                deduped.size(),
                all.size(),
                fetchedBetIds.size()
        );

        return new DiscoveryResult(deduped, calls, fetchedBetIds, apifyFailures, cacheHits);
    }

    private boolean appendBatchSearchResult(
            List<SofaScoreEventDto> all,
            Map<String, Set<Long>> queryToBetIds,
            Set<Long> fetchedBetIds,
            List<String> batch,
            LocalDateTime now) {
        var result = apifySofaScoreClient.searchMatchesBatch(batch);
        if (!result.successful()) {
            log.warn(
                    "Apify search: batch {} zapytań nieudany — nogi z tego batcha spróbują ponownie w kolejnym cyklu",
                    batch.size()
            );
            return false;
        }
        all.addAll(result.matches());
        List<SofaScoreEventDto> cacheable = result.matches().stream()
                .filter(MatchDiscoveryService::isCacheableEvent)
                .toList();
        Map<String, List<SofaScoreEventDto>> toCache = new LinkedHashMap<>();
        for (String q : batch) {
            fetchedBetIds.addAll(queryToBetIds.getOrDefault(q, Set.of()));
            if (!cacheable.isEmpty()) {
                toCache.put(q, cacheable);
            }
        }
        sofaScoreCacheService.putAll(toCache, now);
        return true;
    }

    private static final Set<String> CACHEABLE_STATUS_TYPES = Set.of("finished", "canceled", "postponed");

    private static boolean isCacheableEvent(SofaScoreEventDto event) {
        if (event == null || event.getStatusType() == null) {
            return false;
        }
        return CACHEABLE_STATUS_TYPES.contains(event.getStatusType().toLowerCase(Locale.ROOT));
    }

    private static List<SofaScoreEventDto> dedupEventsByUrl(List<SofaScoreEventDto> events) {
        List<SofaScoreEventDto> deduped = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        for (SofaScoreEventDto event : events) {
            String url = event.getUrl();
            if (url == null || url.isBlank()) {
                deduped.add(event);
            } else if (seenUrls.add(url)) {
                deduped.add(event);
            }
        }
        return deduped;
    }
}
