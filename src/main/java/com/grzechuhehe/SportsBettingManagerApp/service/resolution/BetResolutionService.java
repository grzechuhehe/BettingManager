package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.ApifySofaScoreClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetResolutionService {

    private static final String RESOLUTION_SOURCE = "APIFY_SOFASCORE";

    private final BetRepository betRepository;
    private final ApifySofaScoreClient apifySofaScoreClient;
    private final BetMatcher betMatcher;
    private final BetOutcomeEvaluator betOutcomeEvaluator;

    @Value("${bet.resolution.match-confidence-threshold:0.85}")
    private double confidenceThreshold;

    @Value("${bet.resolution.date-window-days:4}")
    private int dateWindowDays;

    @Value("${bet.resolution.max-bets-per-run:50}")
    private int maxBetsPerRun;

    @Scheduled(fixedRateString = "${bet.resolution.interval-ms:3600000}")
    @Transactional
    public void resolvePendingBets() {
        List<Bet> pending = betRepository.findByStatusAndParentBetIsNull(BetStatus.PENDING);
        log.info("Auto-rozliczanie: {} korzeniowych zakładów PENDING", pending.size());

        Map<String, List<SofaScoreEventDto>> cache = new HashMap<>();
        int processed = 0;
        for (Bet bet : pending) {
            if (processed >= maxBetsPerRun) {
                break;
            }
            try {
                if (bet.getBetType() == BetType.PARLAY) {
                    resolveParlay(bet, cache);
                } else if (resolveSingle(bet, cache)) {
                    betRepository.save(bet);
                }
            } catch (Exception e) {
                log.error("Błąd auto-rozliczania zakładu {}: {}", bet.getId(), e.getMessage());
            }
            processed++;
        }
    }

    private void resolveParlay(Bet parlay, Map<String, List<SofaScoreEventDto>> cache) {
        if (parlay.getChildBets() == null || parlay.getChildBets().isEmpty()) {
            return;
        }
        boolean anyLost = false;
        boolean allDecided = true;
        for (Bet leg : parlay.getChildBets()) {
            if (leg.getStatus() == BetStatus.PENDING) {
                resolveSingle(leg, cache);
            }
            switch (leg.getStatus()) {
                case LOST -> anyLost = true;
                case WON, VOID -> { /* rozstrzygnięta noga */ }
                default -> allDecided = false;
            }
        }
        if (anyLost) {
            applyOutcome(parlay, BetStatus.LOST, null, null);
            betRepository.save(parlay);
        } else if (allDecided) {
            applyOutcome(parlay, BetStatus.WON, null, null);
            betRepository.save(parlay);
        }
    }

    private boolean resolveSingle(Bet bet, Map<String, List<SofaScoreEventDto>> cache) {
        String query = bet.getEventName();
        if (query == null || query.isBlank()) {
            return false;
        }
        List<SofaScoreEventDto> events = cache.computeIfAbsent(
                query.toLowerCase(Locale.ROOT).trim(),
                q -> apifySofaScoreClient.searchMatches(query));

        Optional<BetMatcher.MatchCandidate> match = betMatcher.findBestMatch(bet, events, dateWindowDays);
        if (match.isEmpty() || match.get().confidence() < confidenceThreshold) {
            return false;
        }
        SofaScoreEventDto event = match.get().event();
        Optional<BetStatus> outcome = betOutcomeEvaluator.evaluate(bet, event);
        if (outcome.isEmpty()) {
            return false;
        }
        applyOutcome(bet, outcome.get(), match.get().confidence(), event.getUrl());
        return true;
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
