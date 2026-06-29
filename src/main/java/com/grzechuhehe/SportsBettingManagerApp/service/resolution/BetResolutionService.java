package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.ApifySofaScoreClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.discovery.ResolutionQueuePrioritizer;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetResolutionService {

    private final ApifySofaScoreClient apifySofaScoreClient;
    private final SofaScoreSportMapper sportMapper;
    private final ResolutionNameTranslator nameTranslator;
    private final BetResolutionTransactionService resolutionTx;
    private final SelectionResolvabilityChecker selectionResolvabilityChecker;
    private final SofaScoreCacheService sofaScoreCacheService;
    private final AutoResolutionGuard autoResolutionGuard;
    private final ResolutionQueuePrioritizer resolutionQueuePrioritizer;

    @Value("${bet.resolution.match-confidence-threshold:0.85}")
    private double confidenceThreshold;

    @Value("${bet.resolution.date-window-days:4}")
    private int dateWindowDays;

    @Value("${bet.resolution.max-bets-per-run:50}")
    private int maxBetsPerRun;

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

    @Value("${bet.resolution.search-cooldown-hours:24}")
    private int searchCooldownHours;

    @Value("${bet.resolution.min-hours-after-placed:3}")
    private int minHoursAfterPlaced;

    @Value("${bet.resolution.scheduled-max-days-back:7}")
    private int scheduledMaxDaysBack;

    @Value("${bet.resolution.manual-cooldown-minutes:60}")
    private int manualCooldownMinutes;

    public int getManualCooldownMinutes() {
        return manualCooldownMinutes;
    }

    /** Scheduled cycle: bypasses cooldown (force=true) but never overlaps an in-flight run. */
    @Scheduled(fixedRateString = "${bet.resolution.interval-ms:21600000}")
    public void resolvePendingBets() {
        AutoResolutionGuard.AcquireResult acquire = autoResolutionGuard.tryAcquire(manualCooldownMinutes, true);
        if (acquire.status() != AutoResolutionGuard.Acquisition.ACQUIRED) {
            log.info("Scheduled auto-resolution skipped: {}", acquire.status());
            return;
        }
        boolean success = false;
        try {
            resolvePendingBetsInternal(false);
            success = true;
        } finally {
            autoResolutionGuard.release(success);
        }
    }

    /**
     * Manual trigger. Acquires the shared guard and, if granted, runs the cycle on a
     * virtual thread. Returns the acquisition outcome so the controller can map HTTP status.
     */
    public AutoResolutionGuard.AcquireResult triggerManualResolution(boolean force) {
        AutoResolutionGuard.AcquireResult acquire = autoResolutionGuard.tryAcquire(manualCooldownMinutes, force);
        if (acquire.status() != AutoResolutionGuard.Acquisition.ACQUIRED) {
            return acquire;
        }
        Thread.startVirtualThread(() -> {
            boolean success = false;
            try {
                log.info("Manual auto-resolution started (force={})", force);
                resolvePendingBetsInternal(force);
                success = true;
                log.info("Manual auto-resolution finished (force={})", force);
            } catch (Exception e) {
                log.error("Manual auto-resolution failed: {}", e.getMessage(), e);
            } finally {
                autoResolutionGuard.release(success);
            }
        });
        return acquire;
    }

    private void resolvePendingBetsInternal(boolean force) {
        List<Bet> rootsToProcess = resolutionTx.loadPendingRoots(maxBetsPerRun);
        log.info("Auto-rozliczanie: {} korzeniowych zakładów PENDING (force={})", rootsToProcess.size(), force);

        LocalDateTime now = LocalDateTime.now();
        String cycleId = UUID.randomUUID().toString();
        List<Bet> eligibleLeaves = collectEligibleLeaves(rootsToProcess, now, force);
        Set<Long> eligibleIds = eligibleLeaves.stream().map(Bet::getId).collect(Collectors.toSet());

        long apifyStart = System.currentTimeMillis();
        EventPoolFetch fetch = fetchEventPool(eligibleLeaves, now);
        long apifyMs = System.currentTimeMillis() - apifyStart;
        log.info("Apify HTTP: {} ms, {} wywołań actora (~${} przy $0.08/wywołanie)",
                apifyMs, fetch.apifyCalls(), String.format("%.2f", fetch.apifyCalls() * 0.08));

        Set<Long> fetchedBetIds = fetch.fetchedBetIds();

        if (!eligibleLeaves.isEmpty()) {
            log.info(
                    "Apify kwalifikujące zakłady ({}): {}",
                    eligibleLeaves.size(),
                    eligibleLeaves.stream()
                            .map(b -> b.getId() + "='" + b.getEventName() + "'")
                            .toList()
            );
        }
        log.info(
                "Apify: tryb={}, wywołań={}, meczów w puli={}, kwalifikujących się nóg/zakładów={}",
                apifyMode,
                fetch.apifyCalls(),
                fetch.events().size(),
                eligibleLeaves.size()
        );

        for (Bet root : rootsToProcess) {
            try {
                resolutionTx.processRoot(
                        root.getId(),
                        fetch.events(),
                        now,
                        eligibleIds,
                        fetchedBetIds,
                        confidenceThreshold,
                        dateWindowDays,
                        cycleId
                );
            } catch (Exception e) {
                log.error("Błąd auto-rozliczania zakładu {}: {}", root.getId(), e.getMessage(), e);
            }
        }

        log.info(
                "Apify cycle summary: cycleId={}, eligible={}, cacheHits={}, apifyCalls={}, apifyFailures={}, events={}, fetchedBetIds={}, costUsd={}",
                cycleId,
                eligibleLeaves.size(),
                fetch.cacheHits(),
                fetch.apifyCalls(),
                fetch.apifyFailures(),
                fetch.events().size(),
                fetchedBetIds.size(),
                String.format("%.2f", fetch.apifyCalls() * 0.08)
        );

        int apifyAttempts = fetch.apifyCalls() + fetch.apifyFailures();
        if (apifyAttempts > 0) {
            double failureRate = (double) fetch.apifyFailures() / apifyAttempts;
            if (failureRate > 0.30) {
                log.warn(
                        "Apify cycle {}: failure rate {}% ({} failures / {} batch attempts) exceeds 30% threshold",
                        cycleId,
                        String.format(Locale.ROOT, "%.0f", failureRate * 100),
                        fetch.apifyFailures(),
                        apifyAttempts
                );
            }
        }
    }

    private List<Bet> collectEligibleLeaves(List<Bet> roots, LocalDateTime now, boolean force) {
        List<Bet> eligible = new ArrayList<>();
        for (Bet root : roots) {
            if (root.getBetType() == BetType.PARLAY) {
                if (root.getChildBets() == null) {
                    continue;
                }
                for (Bet leg : root.getChildBets()) {
                    if (leg.getStatus() == BetStatus.PENDING && isEligibleLeaf(leg, now, force)) {
                        eligible.add(leg);
                    }
                }
            } else if (isEligibleLeaf(root, now, force)) {
                eligible.add(root);
            }
        }
        return resolutionQueuePrioritizer.sortByPriority(eligible, roots);
    }

    /** Pojedynczy mecz (SINGLE) lub jedna noga kuponu — nie złożony opis AKO w eventName. */
    private boolean isEligibleLeaf(Bet bet, LocalDateTime now, boolean force) {
        if (bet.getEventName() == null || bet.getEventName().isBlank()) {
            return false;
        }
        if (nameTranslator.resolveQueryForApify(bet.getEventName()).isEmpty()) {
            return false;
        }
        if (!selectionResolvabilityChecker.isAutoResolvable(bet)) {
            return false;
        }
        MarketType market = bet.getMarketType() != null
                ? bet.getMarketType()
                : resolutionTx.inferMarketType(bet);
        if (market == null) {
            return false;
        }
        if (!ResolutionSupportedMarkets.VALUES.contains(market) && !isBetBuilderLeg(bet)) {
            return false;
        }
        if (bet.getPlacedAt() != null && bet.getPlacedAt().isAfter(now.minusHours(minHoursAfterPlaced))) {
            return false;
        }
        if (bet.getLastResolutionAttemptAt() != null
                && bet.getLastResolutionAttemptAt().isAfter(now.minusHours(searchCooldownHours))
                && !force) {
            return false;
        }
        return true;
    }

    private boolean isBetBuilderLeg(Bet bet) {
        String selection = bet.getSelection() == null ? "" : bet.getSelection().toLowerCase(Locale.ROOT);
        return selection.contains("bet builder") || selection.contains("betbuilder")
                || (bet.getBuilderConditionsJson() != null && !bet.getBuilderConditionsJson().isBlank());
    }

    private EventPoolFetch fetchEventPool(List<Bet> eligibleLeaves, LocalDateTime now) {
        if (eligibleLeaves.isEmpty()) {
            return EventPoolFetch.empty();
        }

        if ("search".equalsIgnoreCase(apifyMode)) {
            return fetchBySearchBatch(eligibleLeaves);
        }
        return fetchByScheduled(eligibleLeaves, now);
    }

    private EventPoolFetch fetchByScheduled(List<Bet> eligibleLeaves, LocalDateTime now) {
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
        return new EventPoolFetch(events, 1, fetchedBetIds, 0, 0);
    }

    /**
     * Batch search: unikalne query w jednym lub kilku wywołaniach actora (~$0.08 każde).
     * Wszystkie zebrane query idą do Apify (w paczkach po searchBatchSize), nie tylko pierwsze N.
     */
    private EventPoolFetch fetchBySearchBatch(List<Bet> eligibleLeaves) {
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
            return EventPoolFetch.empty();
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

        return new EventPoolFetch(deduped, calls, fetchedBetIds, apifyFailures, cacheHits);
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
                .filter(BetResolutionService::isCacheableEvent)
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

    private record EventPoolFetch(
            List<SofaScoreEventDto> events,
            int apifyCalls,
            Set<Long> fetchedBetIds,
            int apifyFailures,
            int cacheHits) {

        static EventPoolFetch empty() {
            return new EventPoolFetch(List.of(), 0, Set.of(), 0, 0);
        }
    }
}
