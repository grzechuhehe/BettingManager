package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            MarketType.CORRECT_SCORE
    );

    private final BetRepository betRepository;
    private final BetMatcher betMatcher;
    private final BetOutcomeEvaluator betOutcomeEvaluator;
    private final ResolutionNameTranslator nameTranslator;

    @Transactional(readOnly = true)
    public List<Bet> loadPendingRoots(int limit) {
        return betRepository.findPendingRootsWithLegs(BetStatus.PENDING).stream()
                .limit(limit)
                .toList();
    }

    @Transactional
    public void processRoot(
            Long rootId,
            List<SofaScoreEventDto> eventPool,
            LocalDateTime now,
            Set<Long> eligibleIds,
            double confidenceThreshold,
            int dateWindowDays) {
        Bet root = betRepository.findByIdWithChildBets(rootId).orElse(null);
        if (root == null || root.getStatus() != BetStatus.PENDING) {
            return;
        }
        if (root.getBetType() == BetType.PARLAY) {
            processParlay(root, eventPool, now, eligibleIds, confidenceThreshold, dateWindowDays);
        } else if (eligibleIds.contains(root.getId())) {
            if (resolveSingle(root, eventPool, now, confidenceThreshold, dateWindowDays)) {
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

        for (Bet leg : parlay.getChildBets()) {
            if (leg.getStatus() == BetStatus.PENDING) {
                if (eligibleIds.contains(leg.getId())) {
                    boolean settled = resolveSingle(leg, eventPool, now, confidenceThreshold, dateWindowDays);
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
                } else {
                    log.debug(
                            "Kupon {} — noga {} ({}): PENDING, nie kwalifikuje się do Apify w tym cyklu",
                            parlay.getId(),
                            leg.getId(),
                            leg.getEventName()
                    );
                }
            }

            switch (leg.getStatus()) {
                case WON -> won++;
                case LOST -> lost++;
                case VOID -> voided++;
                default -> pending++;
            }
        }

        int decided = totalLegs - pending;
        log.info(
                "Kupon {}: {}/{} nóg rozstrzygniętych (WON={}, LOST={}, VOID={}, PENDING={})",
                parlay.getId(),
                decided,
                totalLegs,
                won,
                lost,
                voided,
                pending
        );

        if (lost > 0) {
            applyOutcome(parlay, BetStatus.LOST, null, null);
            betRepository.save(parlay);
            log.info("Auto-rozliczono kupon {} → LOST (przynajmniej jedna przegrana noga)", parlay.getId());
            return;
        }

        if (pending == 0 && won + voided == totalLegs) {
            applyOutcome(parlay, BetStatus.WON, null, null);
            betRepository.save(parlay);
            log.info("Auto-rozliczono kupon {} → WON (wszystkie {} nóg wygrane/void)", parlay.getId(), totalLegs);
        } else {
            log.info(
                    "Kupon {} pozostaje PENDING — czekam na rozstrzygnięcie {} nóg",
                    parlay.getId(),
                    pending
            );
        }
    }

    boolean resolveSingle(
            Bet bet,
            List<SofaScoreEventDto> eventPool,
            LocalDateTime now,
            double confidenceThreshold,
            int dateWindowDays) {
        bet.setLastResolutionAttemptAt(now);
        ensureMarketType(bet);

        if (bet.getEventName() == null || bet.getEventName().isBlank()) {
            return false;
        }

        Optional<BetMatcher.MatchCandidate> match = betMatcher.findBestMatch(bet, eventPool, dateWindowDays);
        if (match.isEmpty() || match.get().confidence() < confidenceThreshold) {
            if (match.isPresent()) {
                log.info(
                        "Zakład {} ({}): dopasowanie {} < próg {} → {}",
                        bet.getId(),
                        bet.getEventName(),
                        String.format("%.2f", match.get().confidence()),
                        String.format("%.2f", confidenceThreshold),
                        match.get().event().getHomeTeam() + " vs " + match.get().event().getAwayTeam()
                );
            }
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
            return false;
        }

        applyOutcome(bet, outcome.get(), match.get().confidence(), event.getUrl());
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
        if (selection.contains("over") || selection.contains("under")
                || selection.contains("powyzej") || selection.contains("powyżej")
                || selection.contains("ponizej") || selection.contains("poniżej")) {
            return MarketType.TOTALS_OVER_UNDER;
        }
        return MarketType.MONEYLINE_1X2;
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
