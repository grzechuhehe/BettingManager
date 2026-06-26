package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.ApifySofaScoreClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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

    @Value("${bet.resolution.search-batch-size:15}")
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

    /**
     * Bez @Transactional — Apify trwa minuty; otwarta transakcja zabija połączenie MySQL (Hikari).
     */
    @Scheduled(fixedRateString = "${bet.resolution.interval-ms:21600000}")
    public void resolvePendingBets() {
        resolvePendingBetsInternal(false);
    }

    public void resolvePendingBets(boolean force) {
        resolvePendingBetsInternal(force);
    }

    private void resolvePendingBetsInternal(boolean force) {
        List<Bet> rootsToProcess = resolutionTx.loadPendingRoots(maxBetsPerRun);
        log.info("Auto-rozliczanie: {} korzeniowych zakładów PENDING (force={})", rootsToProcess.size(), force);

        LocalDateTime now = LocalDateTime.now();
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
                        dateWindowDays
                );
            } catch (Exception e) {
                log.error("Błąd auto-rozliczania zakładu {}: {}", root.getId(), e.getMessage(), e);
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
        return eligible;
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
            return new EventPoolFetch(List.of(), 0, Set.of());
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
        return new EventPoolFetch(events, 1, fetchedBetIds);
    }

    /**
     * Batch search: unikalne query w jednym lub kilku wywołaniach actora (~$0.08 każde).
     * Wszystkie zebrane query idą do Apify (w paczkach po searchBatchSize), nie tylko pierwsze N.
     */
    private EventPoolFetch fetchBySearchBatch(List<Bet> eligibleLeaves) {
        LinkedHashSet<String> allQueries = new LinkedHashSet<>();
        java.util.Map<String, Set<Long>> queryToBetIds = new java.util.LinkedHashMap<>();

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
            return new EventPoolFetch(List.of(), 0, Set.of());
        }

        if (allQueries.size() > maxSearchQueries) {
            log.warn(
                    "Apify search: {} unikalnych query (limit konfiguracyjny {} — nadmiarowe i tak pobieramy w batchach)",
                    allQueries.size(),
                    maxSearchQueries
            );
        }

        List<SofaScoreEventDto> all = new ArrayList<>();
        Set<Long> fetchedBetIds = new HashSet<>();
        int calls = 0;
        List<String> batch = new ArrayList<>(searchBatchSize);

        for (String query : allQueries) {
            batch.add(query);
            if (batch.size() >= searchBatchSize) {
                if (calls >= maxApifyCallsPerCycle) {
                    int processedQueries = calls * searchBatchSize;
                    log.warn(
                            "Apify search: limit {} wywołań/cykl — pominięto ~{} zapytań",
                            maxApifyCallsPerCycle,
                            Math.max(0, allQueries.size() - processedQueries)
                    );
                    break;
                }
                if (appendBatchSearchResult(all, queryToBetIds, fetchedBetIds, batch)) {
                    calls++;
                }
                batch.clear();
            }
        }
        if (!batch.isEmpty() && calls < maxApifyCallsPerCycle) {
            if (appendBatchSearchResult(all, queryToBetIds, fetchedBetIds, batch)) {
                calls++;
            }
        }

        log.info(
                "Apify batch search: {} unikalnych query, {} wywołań actora, {} meczów w puli, {} nóg z danymi Apify",
                allQueries.size(),
                calls,
                all.size(),
                fetchedBetIds.size()
        );

        return new EventPoolFetch(all, calls, fetchedBetIds);
    }

    private boolean appendBatchSearchResult(
            List<SofaScoreEventDto> all,
            java.util.Map<String, Set<Long>> queryToBetIds,
            Set<Long> fetchedBetIds,
            List<String> batch) {
        var result = apifySofaScoreClient.searchMatchesBatch(batch);
        if (!result.successful()) {
            log.warn(
                    "Apify search: batch {} zapytań nieudany — nogi z tego batcha spróbują ponownie w kolejnym cyklu",
                    batch.size()
            );
            return false;
        }
        all.addAll(result.matches());
        for (String q : batch) {
            fetchedBetIds.addAll(queryToBetIds.getOrDefault(q, Set.of()));
        }
        return true;
    }

    private record EventPoolFetch(List<SofaScoreEventDto> events, int apifyCalls, Set<Long> fetchedBetIds) {}
}
