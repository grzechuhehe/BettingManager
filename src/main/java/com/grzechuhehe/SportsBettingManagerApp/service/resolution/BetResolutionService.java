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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetResolutionService {

    private static final Set<MarketType> SUPPORTED_MARKETS = Set.of(
            MarketType.MONEYLINE_1X2,
            MarketType.MONEYLINE_12,
            MarketType.TOTALS_OVER_UNDER,
            MarketType.BOTH_TEAMS_TO_SCORE,
            MarketType.CORRECT_SCORE
    );

    private final ApifySofaScoreClient apifySofaScoreClient;
    private final SofaScoreSportMapper sportMapper;
    private final ResolutionNameTranslator nameTranslator;
    private final BetResolutionTransactionService resolutionTx;

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
        List<Bet> rootsToProcess = resolutionTx.loadPendingRoots(maxBetsPerRun);
        log.info("Auto-rozliczanie: {} korzeniowych zakładów PENDING", rootsToProcess.size());

        LocalDateTime now = LocalDateTime.now();
        List<Bet> eligibleLeaves = collectEligibleLeaves(rootsToProcess, now);
        Set<Long> eligibleIds = eligibleLeaves.stream().map(Bet::getId).collect(Collectors.toSet());

        long apifyStart = System.currentTimeMillis();
        EventPoolFetch fetch = fetchEventPool(eligibleLeaves, now);
        long apifyMs = System.currentTimeMillis() - apifyStart;
        log.info("Apify HTTP: {} ms, {} wywołań actora (~${} przy $0.08/wywołanie)",
                apifyMs, fetch.apifyCalls(), String.format("%.2f", fetch.apifyCalls() * 0.08));

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
                        confidenceThreshold,
                        dateWindowDays
                );
            } catch (Exception e) {
                log.error("Błąd auto-rozliczania zakładu {}: {}", root.getId(), e.getMessage(), e);
            }
        }
    }

    private List<Bet> collectEligibleLeaves(List<Bet> roots, LocalDateTime now) {
        List<Bet> eligible = new ArrayList<>();
        for (Bet root : roots) {
            if (root.getBetType() == BetType.PARLAY) {
                // Kupon/AKO: Apify i matcher tylko per noga — rodzic nigdy nie idzie do search
                if (root.getChildBets() == null) {
                    continue;
                }
                for (Bet leg : root.getChildBets()) {
                    if (leg.getStatus() == BetStatus.PENDING && isEligibleLeaf(leg, now)) {
                        eligible.add(leg);
                    }
                }
            } else if (isEligibleLeaf(root, now)) {
                eligible.add(root);
            }
        }
        return eligible;
    }

    /** Pojedynczy mecz (SINGLE) lub jedna noga kuponu — nie złożony opis AKO w eventName. */
    private boolean isEligibleLeaf(Bet bet, LocalDateTime now) {
        if (bet.getEventName() == null || bet.getEventName().isBlank()) {
            return false;
        }
        if (nameTranslator.resolveQueryForApify(bet.getEventName()).isEmpty()) {
            return false;
        }
        MarketType market = bet.getMarketType() != null
                ? bet.getMarketType()
                : resolutionTx.inferMarketType(bet);
        if (market == null || !SUPPORTED_MARKETS.contains(market)) {
            return false;
        }
        if (bet.getPlacedAt() != null && bet.getPlacedAt().isAfter(now.minusHours(minHoursAfterPlaced))) {
            return false;
        }
        if (bet.getLastResolutionAttemptAt() != null
                && bet.getLastResolutionAttemptAt().isAfter(now.minusHours(searchCooldownHours))) {
            return false;
        }
        return true;
    }

    private EventPoolFetch fetchEventPool(List<Bet> eligibleLeaves, LocalDateTime now) {
        if (eligibleLeaves.isEmpty()) {
            return new EventPoolFetch(List.of(), 0);
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
        return new EventPoolFetch(events, 1);
    }

    /**
     * Batch search: wiele query w jednym wywołaniu actora (~$0.08), wspólna pula meczów dla wszystkich nóg.
     */
    private EventPoolFetch fetchBySearchBatch(List<Bet> eligibleLeaves) {
        Set<String> queries = new LinkedHashSet<>();
        for (int i = 0; i < eligibleLeaves.size(); i++) {
            Bet bet = eligibleLeaves.get(i);
            String raw = bet.getEventName().trim();
            Optional<String> queryOpt = nameTranslator.resolveQueryForApify(raw);
            if (queryOpt.isEmpty()) {
                continue;
            }
            String query = queryOpt.get();
            if (queries.add(query)) {
                log.info("Apify search query (noga/zakład {}): '{}' → '{}'", bet.getId(), raw, query);
            }
            if (queries.size() >= maxSearchQueries) {
                log.warn(
                        "Apify search: limit {} unikalnych query — pominięto {} kolejnych zakładów",
                        maxSearchQueries,
                        eligibleLeaves.size() - i - 1
                );
                break;
            }
        }

        if (queries.isEmpty()) {
            log.warn("Apify search: brak zapytań ({} kwalifikujących)", eligibleLeaves.size());
            return new EventPoolFetch(List.of(), 0);
        }

        log.info("Apify batch search: {} unikalnych query w {} batch(ach) po max {}",
                queries.size(),
                (queries.size() + searchBatchSize - 1) / searchBatchSize,
                searchBatchSize);

        List<SofaScoreEventDto> all = new ArrayList<>();
        int calls = 0;
        List<String> batch = new ArrayList<>(searchBatchSize);
        for (String query : queries) {
            batch.add(query);
            if (batch.size() >= searchBatchSize) {
                all.addAll(apifySofaScoreClient.searchMatchesBatch(batch));
                calls++;
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            all.addAll(apifySofaScoreClient.searchMatchesBatch(batch));
            calls++;
        }
        return new EventPoolFetch(all, calls);
    }

    private record EventPoolFetch(List<SofaScoreEventDto> events, int apifyCalls) {}
}
