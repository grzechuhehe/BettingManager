package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
class BetResolutionTransactionService {

    private static final String RESOLUTION_SOURCE = "APIFY_SOFASCORE";
    private static final Set<MarketType> SUPPORTED_MARKETS = Set.of(
            MarketType.MONEYLINE_1X2,
            MarketType.MONEYLINE_12,
            MarketType.TOTALS_OVER_UNDER,
            MarketType.BOTH_TEAMS_TO_SCORE,
            MarketType.CORRECT_SCORE,
            MarketType.HANDICAP,
            MarketType.ASIAN_HANDICAP
    );

    private final BetRepository betRepository;
    private final BetMatcher betMatcher;
    private final BetOutcomeEvaluator betOutcomeEvaluator;
    private final ResolutionNameTranslator nameTranslator;
    private final SelectionResolvabilityChecker selectionResolvabilityChecker;

    @Transactional(readOnly = true)
    public List<Bet> loadPendingRoots(int limit) {
        // Limit po stronie bazy (najnowsze zakłady pierwsze), potem dociągnięcie nóg.
        List<Long> rootIds = betRepository.findPendingRootIds(BetStatus.PENDING, PageRequest.of(0, limit));
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
            int dateWindowDays) {
        Bet root = betRepository.findByIdWithChildBets(rootId).orElse(null);
        if (root == null || root.getStatus() != BetStatus.PENDING) {
            return;
        }
        if (root.getBetType() == BetType.PARLAY) {
            processParlay(root, eventPool, now, eligibleIds, fetchedBetIds, confidenceThreshold, dateWindowDays);
        } else if (eligibleIds.contains(root.getId())) {
            if (resolveSingle(root, eventPool, now, fetchedBetIds, confidenceThreshold, dateWindowDays)) {
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
            int dateWindowDays) {
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
                            "Kupon {} — noga {} ({}): pominięta (cooldown / niedozwolony typ selekcji)",
                            parlay.getId(),
                            leg.getId(),
                            leg.getEventName()
                    );
                } else if (!fetchedBetIds.contains(leg.getId())) {
                    log.info(
                            "Kupon {} — noga {} ({}): query nie weszło do Apify w tym cyklu — czekam",
                            parlay.getId(),
                            leg.getId(),
                            leg.getEventName()
                    );
                } else {
                    boolean settled = resolveSingle(leg, eventPool, now, fetchedBetIds, confidenceThreshold, dateWindowDays);
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
     * Kupon z nogami VOID: kurs efektywny = iloczyn kursów nóg WON (VOID liczone jak 1.0).
     * Gdy brak kursów nóg, fallback do pełnego kursu kuponu (potentialWinnings).
     */
    private void applyParlayWonWithVoids(Bet parlay) {
        BigDecimal effectiveOdds = BigDecimal.ONE;
        boolean hasLegOdds = false;
        if (parlay.getChildBets() != null) {
            for (Bet leg : parlay.getChildBets()) {
                if (leg.getStatus() == BetStatus.WON && leg.getOdds() != null) {
                    effectiveOdds = effectiveOdds.multiply(leg.getOdds());
                    hasLegOdds = true;
                }
            }
        }
        parlay.setStatus(BetStatus.WON);
        parlay.setSettledAt(LocalDateTime.now());
        parlay.setResolutionSource(RESOLUTION_SOURCE);
        if (hasLegOdds && parlay.getStake() != null) {
            BigDecimal payout = parlay.getStake().multiply(effectiveOdds);
            parlay.setFinalProfit(payout.subtract(parlay.getStake()));
        } else if (parlay.getPotentialWinnings() != null && parlay.getStake() != null) {
            parlay.setFinalProfit(parlay.getPotentialWinnings().subtract(parlay.getStake()));
        }
    }

    boolean resolveSingle(
            Bet bet,
            List<SofaScoreEventDto> eventPool,
            LocalDateTime now,
            Set<Long> fetchedBetIds,
            double confidenceThreshold,
            int dateWindowDays) {
        if (!fetchedBetIds.contains(bet.getId())) {
            return false;
        }
        if (eventPool == null || eventPool.isEmpty()) {
            log.debug("Zakład {}: pusta pula Apify — bez cooldownu, spróbujemy w kolejnym cyklu", bet.getId());
            return false;
        }

        ensureMarketType(bet);

        if (bet.getEventName() == null || bet.getEventName().isBlank()) {
            return false;
        }

        if (!selectionResolvabilityChecker.isAutoResolvable(bet)) {
            log.info(
                    "Zakład {} ({}): selekcja wymaga ręcznego rozliczenia — pomijam",
                    bet.getId(),
                    bet.getSelection()
            );
            return false;
        }

        Optional<BetMatcher.MatchCandidate> match = betMatcher.findBestMatch(bet, eventPool, dateWindowDays);
        if (match.isEmpty()) {
            log.info(
                    "Zakład {} ({}): brak dopasowania w puli {} meczów Apify",
                    bet.getId(),
                    bet.getEventName(),
                    eventPool.size()
            );
            return false;
        }
        if (match.get().confidence() < confidenceThreshold) {
            bet.setLastResolutionAttemptAt(now);
            log.info(
                    "Zakład {} ({}): dopasowanie {} < próg {} → {}",
                    bet.getId(),
                    bet.getEventName(),
                    String.format("%.2f", match.get().confidence()),
                    String.format("%.2f", confidenceThreshold),
                    match.get().event().getHomeTeam() + " vs " + match.get().event().getAwayTeam()
            );
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

        SofaScoreEventDto event = match.get().event();
        Optional<BetStatus> outcome = betOutcomeEvaluator.evaluate(bet, event);
        if (outcome.isEmpty()) {
            bet.setLastResolutionAttemptAt(now);
            return false;
        }

        applyOutcome(bet, outcome.get(), match.get().confidence(), event.getUrl());
        bet.setLastResolutionAttemptAt(now);
        return true;
    }

    MarketType inferMarketType(Bet bet) {
        if (bet.getMarketType() != null || bet.getEventName() == null) {
            return bet.getMarketType();
        }
        if (nameTranslator.resolveQueryForApify(bet.getEventName()).isEmpty()) {
            return null;
        }
        String selection = bet.getSelection() == null ? "" : bet.getSelection().toLowerCase(Locale.ROOT);
        if (isTennisBet(bet)) {
            if (selection.matches(".*\\([+-]?\\d+(?:[.,]\\d+)?\\).*")) {
                return MarketType.HANDICAP;
            }
            return MarketType.MONEYLINE_12;
        }
        if (selection.matches(".*\\([+-]?\\d+(?:[.,]\\d+)?\\).*")) {
            return MarketType.HANDICAP;
        }
        if (selection.contains("over") || selection.contains("under")
                || selection.contains("powyzej") || selection.contains("powyżej")
                || selection.contains("ponizej") || selection.contains("poniżej")) {
            return MarketType.TOTALS_OVER_UNDER;
        }
        return MarketType.MONEYLINE_1X2;
    }

    private boolean isTennisBet(Bet bet) {
        if (bet.getSport() != null && !bet.getSport().isBlank()) {
            String sport = bet.getSport().toLowerCase(Locale.ROOT);
            return sport.contains("tennis") || sport.contains("tenis");
        }
        return looksLikeTwoPlayerNames(bet.getEventName());
    }

    private boolean looksLikeTwoPlayerNames(String eventName) {
        if (eventName == null) {
            return false;
        }
        return nameTranslator.parseTwoTeamSides(eventName)
                .map(sides -> sides.home().contains(",") && sides.away().contains(","))
                .orElse(false);
    }

    private void ensureMarketType(Bet bet) {
        if (bet.getMarketType() != null) {
            return;
        }
        MarketType inferred = inferMarketType(bet);
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
