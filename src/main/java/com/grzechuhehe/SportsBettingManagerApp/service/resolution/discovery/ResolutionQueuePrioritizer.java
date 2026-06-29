package com.grzechuhehe.SportsBettingManagerApp.service.resolution.discovery;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ResolutionQueuePrioritizer {

    private static final double NEAR_MISS_CONFIDENCE_THRESHOLD = 0.75;
    private static final int NEAR_MISS_SCORE = 100;
    private static final int STALE_EVENT_SCORE = 50;
    private static final int WON_SIBLING_SCORE = 30;

    public List<Bet> sortByPriority(List<Bet> legs, List<Bet> pendingRoots) {
        if (legs == null || legs.isEmpty()) {
            return List.of();
        }
        Map<Long, Bet> legToRoot = buildLegToRootMap(pendingRoots);
        LocalDateTime now = LocalDateTime.now();
        return legs.stream()
                .sorted(Comparator
                        .comparingInt((Bet leg) -> score(leg, legToRoot, now)).reversed()
                        .thenComparing(Bet::getId))
                .toList();
    }

    private int score(Bet leg, Map<Long, Bet> legToRoot, LocalDateTime now) {
        int score = 0;
        if (leg.getMatchConfidence() != null
                && leg.getMatchConfidence() >= NEAR_MISS_CONFIDENCE_THRESHOLD) {
            score += NEAR_MISS_SCORE;
        }
        if (leg.getEventDate() != null && leg.getEventDate().isBefore(now.minusHours(48))) {
            score += STALE_EVENT_SCORE;
        }
        Bet root = legToRoot.get(leg.getId());
        if (root == null && leg.getParentBet() != null) {
            root = leg.getParentBet();
        }
        if (root != null && hasWonSibling(leg, root)) {
            score += WON_SIBLING_SCORE;
        }
        return score;
    }

    private boolean hasWonSibling(Bet leg, Bet root) {
        if (root.getChildBets() == null) {
            return false;
        }
        for (Bet sibling : root.getChildBets()) {
            if (!sibling.getId().equals(leg.getId()) && sibling.getStatus() == BetStatus.WON) {
                return true;
            }
        }
        return false;
    }

    private Map<Long, Bet> buildLegToRootMap(List<Bet> pendingRoots) {
        Map<Long, Bet> map = new HashMap<>();
        if (pendingRoots == null) {
            return map;
        }
        for (Bet root : pendingRoots) {
            if (root.getBetType() == BetType.PARLAY && root.getChildBets() != null) {
                for (Bet leg : root.getChildBets()) {
                    map.put(leg.getId(), root);
                }
            } else {
                map.put(root.getId(), root);
            }
        }
        return map;
    }
}
