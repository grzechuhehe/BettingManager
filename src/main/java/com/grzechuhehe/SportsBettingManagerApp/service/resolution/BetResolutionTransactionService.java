package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.BetResolutionAttempt;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetResolutionAttemptRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.importing.MarketTypeInferrer;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.SportConfidenceThresholds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
class BetResolutionTransactionService {

    private static final String RESOLUTION_SOURCE = "APIFY_SOFASCORE";

    private final BetRepository betRepository;
    private final BetResolutionAttemptRepository attemptRepository;
    private final BetMatcher betMatcher;
    private final BetOutcomeEvaluator betOutcomeEvaluator;
    private final SelectionResolvabilityChecker selectionResolvabilityChecker;
    private final SportConfidenceThresholds sportConfidenceThresholds;
    private final EventEnrichmentService enrichmentService;
    private final MarketTypeInferrer marketTypeInferrer;

    @Transactional(readOnly = true)
    public List<Bet> loadPendingRoots(int limit) {
        return loadPendingRoots(ResolutionRunConfig.defaultNewest(limit, 4));
    }

    @Transactional(readOnly = true)
    public List<Bet> loadPendingRoots(ResolutionRunConfig config) {
        List<Long> rootIds = switch (config.queueMode()) {
            case NEWEST_FIRST -> betRepository.findPendingRootIds(
                    BetStatus.PENDING, PageRequest.of(0, config.limit()));
            case OLDEST_BEFORE_CUTOFF -> {
                if (config.placedBeforeCutoff() == null) {
                    throw new IllegalArgumentException("placedBeforeCutoff required for OLDEST_BEFORE_CUTOFF");
                }
                yield betRepository.findPendingRootIdsBeforeCutoff(
                        BetStatus.PENDING, config.placedBeforeCutoff(), PageRequest.of(0, config.limit()));
            }
        };
        if (rootIds.isEmpty()) {
            return List.of();
        }
        return betRepository.findRootsWithLegsByIds(rootIds);
    }

    @Transactional
    public void processRoot(
            Long rootId,
            List<SofaScoreEventDto> eventPool,
            LocalDateTime now,
            Set<Long> eligibleIds,
            Set<Long> fetchedBetIds,
            double confidenceThreshold,
            int dateWindowDays,
            CycleEnrichmentBudget enrichmentBudget,
            String cycleId) {
        Bet root = betRepository.findByIdWithChildBets(rootId).orElse(null);
        if (root == null || root.getStatus() != BetStatus.PENDING) {
            return;
        }
        if (root.getBetType() == BetType.PARLAY) {
            processParlay(root, eventPool, now, eligibleIds, fetchedBetIds, confidenceThreshold, dateWindowDays, enrichmentBudget, cycleId);
        } else if (eligibleIds.contains(root.getId())) {
            if (resolveSingle(root, eventPool, now, fetchedBetIds, confidenceThreshold, dateWindowDays, enrichmentBudget, cycleId)) {
                log.info("Auto-rozliczono zakład {} → {}", root.getId(), root.getStatus());
            }
            betRepository.save(root);
        }
    }

    private void processParlay(
            Bet parlay,
            List<SofaScoreEventDto> eventPool,
            LocalDateTime now,
            Set<Long> eligibleIds,
            Set<Long> fetchedBetIds,
            double confidenceThreshold,
            int dateWindowDays,
            CycleEnrichmentBudget enrichmentBudget,
            String cycleId) {
        if (parlay.getChildBets() == null || parlay.getChildBets().isEmpty()) {
            log.warn("Kupon {}: brak nóg — pomijam auto-rozliczanie", parlay.getId());
            return;
        }

        int totalLegs = parlay.getChildBets().size();
        int won = 0;
        int lost = 0;
        int voided = 0;
        int pending = 0;
        int ambiguous = 0; // CASHED_OUT / HALF_WON / HALF_LOST — wymaga ręcznej decyzji

        for (Bet leg : parlay.getChildBets()) {
            if (leg.getStatus() == BetStatus.PENDING) {
                if (!eligibleIds.contains(leg.getId())) {
                    log.debug(
                            "Kupon {} — noga {} ({}): pominięta ({})",
                            parlay.getId(),
                            leg.getId(),
                            leg.getEventName(),
                            leg.getResolutionBlockingReason() != null
                                    ? leg.getResolutionBlockingReason().name()
                                    : "UNKNOWN");
                } else if (!fetchedBetIds.contains(leg.getId())) {
                    log.info(
                            "Kupon {} — noga {} ({}): query nie weszło do Apify w tym cyklu — czekam",
                            parlay.getId(),
                            leg.getId(),
                            leg.getEventName()
                    );
                } else {
                    boolean settled = resolveSingle(
                            leg, eventPool, now, fetchedBetIds, confidenceThreshold, dateWindowDays, enrichmentBudget, cycleId);
                    betRepository.save(leg);
                    if (settled) {
                        log.info(
                                "Kupon {} — noga {} ({}): rozstrzygnięto → {}",
                                parlay.getId(),
                                leg.getId(),
                                leg.getEventName(),
                                leg.getStatus()
                        );
                    }
                }
            }

            switch (leg.getStatus()) {
                case WON -> won++;
                case LOST -> lost++;
                case VOID -> voided++;
                case PENDING -> pending++;
                default -> ambiguous++; // CASHED_OUT, HALF_WON, HALF_LOST
            }
        }

        int decided = totalLegs - pending;
        log.info(
                "Kupon {}: {}/{} nóg rozstrzygniętych (WON={}, LOST={}, VOID={}, PENDING={}, inne={})",
                parlay.getId(),
                decided,
                totalLegs,
                won,
                lost,
                voided,
                pending,
                ambiguous
        );

        // Przegrana noga przesądza o kuponie nawet gdy reszta jeszcze PENDING.
        if (lost > 0) {
            applyOutcome(parlay, BetStatus.LOST, null, null);
            betRepository.save(parlay);
            log.info("Auto-rozliczono kupon {} → LOST (przynajmniej jedna przegrana noga)", parlay.getId());
            return;
        }

        // Stany niejednoznaczne (HALF_*/CASHED_OUT) — nie potrafimy bezpiecznie auto-rozliczyć.
        if (ambiguous > 0) {
            log.warn(
                    "Kupon {}: {} nóg w stanie niejednoznacznym (HALF_*/CASHED_OUT) — wymaga ręcznego rozliczenia",
                    parlay.getId(),
                    ambiguous
            );
            return;
        }

        if (pending > 0) {
            log.info("Kupon {} pozostaje PENDING — czekam na rozstrzygnięcie {} nóg", parlay.getId(), pending);
            return;
        }

        if (won == totalLegs) {
            applyOutcome(parlay, BetStatus.WON, null, null);
            betRepository.save(parlay);
            log.info("Auto-rozliczono kupon {} → WON (wszystkie {} nóg wygrane)", parlay.getId(), totalLegs);
            return;
        }

        if (voided == totalLegs) {
            applyOutcome(parlay, BetStatus.VOID, null, null);
            betRepository.save(parlay);
            log.info("Auto-rozliczono kupon {} → VOID (wszystkie {} nóg zwrócone)", parlay.getId(), totalLegs);
            return;
        }

        if (lost == 0 && pending == 0 && won > 0 && voided > 0) {
            if (!allWonLegsHaveOdds(parlay) || parlay.getStake() == null) {
                log.warn(
                        "Kupon {} (WON={}, VOID={}): noga WON bez kursu lub brak stawki — "
                                + "nie można policzyć kursu efektywnego, wymaga ręcznego rozliczenia",
                        parlay.getId(), won, voided);
                return;
            }
            applyParlayWonWithVoids(parlay);
            betRepository.save(parlay);
            log.info(
                    "Auto-rozliczono kupon {} → WON (WON={}, VOID={} — nogi VOID pominięte w kursie)",
                    parlay.getId(), won, voided);
            return;
        }

        log.info(
                "Kupon {} pozostaje PENDING — nie wszystkie nogi WON (WON={}, VOID={}, LOST={}, total={})",
                parlay.getId(),
                won,
                voided,
                lost,
                totalLegs
        );
    }

    /**
     * Czy każda noga WON kuponu ma ustawiony kurs (odds). Wymagane, by policzyć
     * poprawny kurs efektywny przy obecności nóg VOID — bez tego nie auto-rozliczamy.
     */
    private boolean allWonLegsHaveOdds(Bet parlay) {
        if (parlay.getChildBets() == null) {
            return false;
        }
        for (Bet leg : parlay.getChildBets()) {
            if (leg.getStatus() == BetStatus.WON && leg.getOdds() == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Kupon z nogami VOID: kurs efektywny = iloczyn kursów nóg WON (VOID liczone jak 1.0).
     * Zakłada, że każda noga WON ma ustawiony kurs oraz {@code stake != null}
     * (sprawdzone przez {@link #allWonLegsHaveOdds(Bet)} przed wywołaniem).
     */
    private void applyParlayWonWithVoids(Bet parlay) {
        BigDecimal effectiveOdds = BigDecimal.ONE;
        if (parlay.getChildBets() != null) {
            for (Bet leg : parlay.getChildBets()) {
                if (leg.getStatus() == BetStatus.WON && leg.getOdds() != null) {
                    effectiveOdds = effectiveOdds.multiply(leg.getOdds());
                }
            }
        }
        parlay.setStatus(BetStatus.WON);
        parlay.setSettledAt(LocalDateTime.now());
        parlay.setResolutionSource(RESOLUTION_SOURCE);
        BigDecimal payout = parlay.getStake().multiply(effectiveOdds);
        parlay.setFinalProfit(payout.subtract(parlay.getStake()));
    }

    boolean resolveSingle(
            Bet bet,
            List<SofaScoreEventDto> eventPool,
            LocalDateTime now,
            Set<Long> fetchedBetIds,
            double confidenceThreshold,
            int dateWindowDays,
            CycleEnrichmentBudget enrichmentBudget,
            String cycleId) {
        boolean apifyDataAvailable = fetchedBetIds.contains(bet.getId());
        Optional<BetMatcher.MatchCandidate> match = Optional.empty();
        String errorCode = "SUCCESS";

        if (!apifyDataAvailable) {
            errorCode = "NO_APIFY_DATA";
            recordAttempt(cycleId, bet.getId(), apifyDataAvailable, match, errorCode, now, false);
            return false;
        }
        if (eventPool == null || eventPool.isEmpty()) {
            log.debug("Zakład {}: pusta pula Apify — bez cooldownu, spróbujemy w kolejnym cyklu", bet.getId());
            errorCode = "EMPTY_POOL";
            recordAttempt(cycleId, bet.getId(), apifyDataAvailable, match, errorCode, now, false);
            return false;
        }

        ensureMarketType(bet);

        if (bet.getEventName() == null || bet.getEventName().isBlank()) {
            errorCode = "NO_EVENT_NAME";
            recordAttempt(cycleId, bet.getId(), apifyDataAvailable, match, errorCode, now, false);
            return false;
        }

        if (!selectionResolvabilityChecker.isAutoResolvable(bet)) {
            log.info(
                    "Zakład {} ({}): selekcja wymaga ręcznego rozliczenia — pomijam",
                    bet.getId(),
                    bet.getSelection()
            );
            errorCode = "MANUAL_ONLY";
            recordAttempt(cycleId, bet.getId(), apifyDataAvailable, match, errorCode, now, false);
            return false;
        }

        match = betMatcher.findBestMatch(bet, eventPool, dateWindowDays);
        if (match.isEmpty()) {
            log.info(
                    "Zakład {} ({}): brak dopasowania w puli {} meczów Apify",
                    bet.getId(),
                    bet.getEventName(),
                    eventPool.size()
            );
            errorCode = "NO_MATCH";
            recordAttempt(cycleId, bet.getId(), apifyDataAvailable, match, errorCode, now, false);
            return false;
        }
        double threshold = sportConfidenceThresholds.forBet(bet);
        if (match.get().confidence() < threshold) {
            bet.setLastResolutionAttemptAt(now);
            log.info(
                    "Zakład {} ({}): dopasowanie {} < próg {} → {}",
                    bet.getId(),
                    bet.getEventName(),
                    String.format("%.2f", match.get().confidence()),
                    String.format("%.2f", threshold),
                    match.get().event().getHomeTeam() + " vs " + match.get().event().getAwayTeam()
            );
            errorCode = "BELOW_THRESHOLD";
            recordAttempt(cycleId, bet.getId(), apifyDataAvailable, match, errorCode, now, false);
            return false;
        }

        log.info(
                "Zakład {} ({}): dopasowano {} vs {} (confidence={})",
                bet.getId(),
                bet.getEventName(),
                match.get().event().getHomeTeam(),
                match.get().event().getAwayTeam(),
                String.format("%.2f", match.get().confidence())
        );

        int enrichmentUsedBefore = enrichmentBudget.usedCount();
        SofaScoreEventDto eventForEval = enrichmentService.enrichIfNeeded(
                bet, match.get().event(), match.get().confidence(), enrichmentBudget);
        boolean enrichmentAttempted = enrichmentBudget.usedCount() > enrichmentUsedBefore;
        Optional<BetStatus> outcome = betOutcomeEvaluator.evaluate(bet, eventForEval);
        if (outcome.isEmpty()) {
            bet.setLastResolutionAttemptAt(now);
            errorCode = "UNRESOLVED_MARKET";
            recordAttempt(cycleId, bet.getId(), apifyDataAvailable, match, errorCode, now, enrichmentAttempted);
            return false;
        }

        applyOutcome(bet, outcome.get(), match.get().confidence(), eventForEval.getUrl());
        bet.setLastResolutionAttemptAt(now);
        recordAttempt(cycleId, bet.getId(), apifyDataAvailable, match, errorCode, now, enrichmentAttempted);
        return true;
    }

    private void recordAttempt(
            String cycleId,
            Long betId,
            boolean apifyDataAvailable,
            Optional<BetMatcher.MatchCandidate> match,
            String errorCode,
            LocalDateTime now,
            boolean enrichmentAttempted) {
        if (cycleId == null) {
            return;
        }
        BetResolutionAttempt attempt = new BetResolutionAttempt();
        attempt.setBetId(betId);
        attempt.setCycleId(cycleId);
        attempt.setApifyDataAvailable(apifyDataAvailable);
        attempt.setMatchFound(match.isPresent());
        attempt.setMatchConfidence(match.map(BetMatcher.MatchCandidate::confidence).orElse(null));
        attempt.setErrorCode(errorCode);
        attempt.setPhase(ResolutionPhase.SETTLEMENT);
        attempt.setEnrichmentAttempted(enrichmentAttempted);
        attempt.setAttemptedAt(now);
        attemptRepository.save(attempt);
    }

    private void ensureMarketType(Bet bet) {
        if (bet.getMarketType() != null) {
            return;
        }
        MarketType inferred = marketTypeInferrer.infer(bet);
        if (inferred != null) {
            bet.setMarketType(inferred);
        }
    }

    private void applyOutcome(Bet bet, BetStatus status, Double confidence, String eventUrl) {
        bet.setStatus(status);
        bet.setSettledAt(LocalDateTime.now());
        bet.setResolutionSource(RESOLUTION_SOURCE);
        if (confidence != null) {
            bet.setMatchConfidence(confidence);
        }
        if (eventUrl != null) {
            bet.setResolvedEventUrl(eventUrl);
        }
        if (status == BetStatus.WON && bet.getPotentialWinnings() != null && bet.getStake() != null) {
            bet.setFinalProfit(bet.getPotentialWinnings().subtract(bet.getStake()));
        } else if (status == BetStatus.LOST && bet.getStake() != null) {
            bet.setFinalProfit(bet.getStake().negate());
        } else if (status == BetStatus.VOID) {
            bet.setFinalProfit(BigDecimal.ZERO);
        }
    }
}
