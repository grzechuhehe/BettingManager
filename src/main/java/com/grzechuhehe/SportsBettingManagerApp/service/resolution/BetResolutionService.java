package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.BetResolutionAttempt;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetResolutionAttemptRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.discovery.DiscoveryResult;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.discovery.MatchDiscoveryService;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.discovery.ResolutionQueuePrioritizer;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetResolutionService {

    private final ResolutionNameTranslator nameTranslator;
    private final BetResolutionTransactionService resolutionTx;
    private final SelectionResolvabilityChecker selectionResolvabilityChecker;
    private final AutoResolutionGuard autoResolutionGuard;
    private final ResolutionQueuePrioritizer resolutionQueuePrioritizer;
    private final MatchDiscoveryService discoveryService;
    private final BetResolutionAttemptRepository attemptRepository;
    private final ResolutionCycleMetricsHolder metricsHolder;

    private static final double APIFY_COST_PER_CALL_USD = 0.08;

    @Value("${bet.resolution.match-confidence-threshold:0.85}")
    private double confidenceThreshold;

    @Value("${bet.resolution.date-window-days:4}")
    private int dateWindowDays;

    @Value("${bet.resolution.max-bets-per-run:50}")
    private int maxBetsPerRun;

    @Value("${bet.resolution.search-cooldown-hours:24}")
    private int searchCooldownHours;

    @Value("${bet.resolution.min-hours-after-placed:3}")
    private int minHoursAfterPlaced;

    @Value("${bet.resolution.manual-cooldown-minutes:60}")
    private int manualCooldownMinutes;

    @Value("${bet.resolution.max-enrichment-calls-per-cycle:3}")
    private int maxEnrichmentCallsPerCycle;

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
        DiscoveryResult fetch = discoveryService.discover(eligibleLeaves, now);
        long apifyMs = System.currentTimeMillis() - apifyStart;
        log.info("Apify HTTP: {} ms, {} wywołań actora (~${} przy $0.08/wywołanie)",
                apifyMs, fetch.apifyCalls(), String.format("%.2f", fetch.apifyCalls() * 0.08));

        Set<Long> fetchedBetIds = fetch.fetchedBetIds();
        CycleEnrichmentBudget enrichmentBudget = new CycleEnrichmentBudget(maxEnrichmentCallsPerCycle);

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
                discoveryService.getApifyMode(),
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
                        enrichmentBudget,
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

        storeCycleMetrics(cycleId, eligibleLeaves.size(), fetch, enrichmentBudget);
    }

    private void storeCycleMetrics(
            String cycleId,
            int eligible,
            DiscoveryResult fetch,
            CycleEnrichmentBudget enrichmentBudget) {
        int discoveryCalls = fetch.apifyCalls();
        int enrichmentCalls = enrichmentBudget.usedCount();
        double estimatedCostUsd = (discoveryCalls + enrichmentCalls) * APIFY_COST_PER_CALL_USD;

        List<BetResolutionAttempt> attempts = attemptRepository.findByCycleId(cycleId);
        int settled = 0;
        int belowThreshold = 0;
        int noMatch = 0;
        for (BetResolutionAttempt attempt : attempts) {
            if ("SUCCESS".equals(attempt.getErrorCode())) {
                settled++;
            } else if ("BELOW_THRESHOLD".equals(attempt.getErrorCode())) {
                belowThreshold++;
            } else if ("NO_MATCH".equals(attempt.getErrorCode())) {
                noMatch++;
            }
        }

        ResolutionCycleMetrics metrics = new ResolutionCycleMetrics(
                cycleId,
                eligible,
                discoveryCalls,
                enrichmentCalls,
                settled,
                belowThreshold,
                noMatch,
                estimatedCostUsd);
        metricsHolder.setLast(metrics);

        log.info(
                "Resolution cycle metrics: cycleId={}, eligible={}, discoveryCalls={}, enrichmentCalls={}, "
                        + "settled={}, belowThreshold={}, noMatch={}, estimatedCostUsd={}",
                cycleId,
                eligible,
                discoveryCalls,
                enrichmentCalls,
                settled,
                belowThreshold,
                noMatch,
                String.format(Locale.ROOT, "%.2f", estimatedCostUsd));
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
            return rejectEligible(bet, ResolutionBlockingReason.NOT_SEARCHABLE_EVENT);
        }
        if (nameTranslator.resolveQueryForApify(bet.getEventName()).isEmpty()) {
            return rejectEligible(bet, ResolutionBlockingReason.NOT_SEARCHABLE_EVENT);
        }
        if (!selectionResolvabilityChecker.isAutoResolvable(bet)) {
            return rejectEligible(bet, ResolutionBlockingReason.MANUAL_ONLY_SELECTION);
        }
        MarketType market = bet.getMarketType() != null
                ? bet.getMarketType()
                : resolutionTx.inferMarketType(bet);
        if (market == MarketType.OUTRIGHT) {
            return rejectEligible(bet, ResolutionBlockingReason.OUTRIGHT_UNSUPPORTED);
        }
        if (market == null
                || (!ResolutionSupportedMarkets.VALUES.contains(market) && !isBetBuilderLeg(bet))) {
            return rejectEligible(bet, ResolutionBlockingReason.UNSUPPORTED_MARKET);
        }
        if (bet.getPlacedAt() != null && bet.getPlacedAt().isAfter(now.minusHours(minHoursAfterPlaced))) {
            return rejectEligible(bet, ResolutionBlockingReason.TOO_RECENT);
        }
        if (bet.getLastResolutionAttemptAt() != null
                && bet.getLastResolutionAttemptAt().isAfter(now.minusHours(searchCooldownHours))
                && !force) {
            return rejectEligible(bet, ResolutionBlockingReason.COOLDOWN);
        }
        bet.setResolutionBlockingReason(null);
        return true;
    }

    private static boolean rejectEligible(Bet bet, ResolutionBlockingReason reason) {
        bet.setResolutionBlockingReason(reason);
        return false;
    }

    private boolean isBetBuilderLeg(Bet bet) {
        String selection = bet.getSelection() == null ? "" : bet.getSelection().toLowerCase(Locale.ROOT);
        return selection.contains("bet builder") || selection.contains("betbuilder")
                || (bet.getBuilderConditionsJson() != null && !bet.getBuilderConditionsJson().isBlank());
    }
}
