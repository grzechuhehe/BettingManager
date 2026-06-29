package com.grzechuhehe.SportsBettingManagerApp.service.resolution.discovery;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResolutionQueuePrioritizerTest {

    private final ResolutionQueuePrioritizer prioritizer = new ResolutionQueuePrioritizer();

    @Test
    void nearMissLegSortsBeforeFreshLeg() {
        LocalDateTime now = LocalDateTime.now();
        Bet nearMiss = Bet.builder()
                .id(1L)
                .matchConfidence(0.78)
                .eventDate(now.minusHours(1))
                .build();
        Bet fresh = Bet.builder()
                .id(2L)
                .eventDate(now.minusHours(1))
                .build();

        List<Bet> sorted = prioritizer.sortByPriority(List.of(fresh, nearMiss), List.of());

        assertEquals(1L, sorted.get(0).getId());
        assertEquals(2L, sorted.get(1).getId());
    }

    @Test
    void staleEventLegSortsBeforeRecentEventLeg() {
        LocalDateTime now = LocalDateTime.now();
        Bet stale = Bet.builder()
                .id(10L)
                .eventDate(now.minusHours(72))
                .build();
        Bet recent = Bet.builder()
                .id(11L)
                .eventDate(now.minusHours(12))
                .build();

        List<Bet> sorted = prioritizer.sortByPriority(List.of(recent, stale), List.of());

        assertEquals(10L, sorted.get(0).getId());
        assertEquals(11L, sorted.get(1).getId());
    }

    @Test
    void parlayLegWithWonSiblingSortsBeforeOtherPendingLeg() {
        LocalDateTime now = LocalDateTime.now();
        Bet parlay = Bet.builder()
                .id(100L)
                .betType(BetType.PARLAY)
                .status(BetStatus.PENDING)
                .build();
        Bet blockingLeg = Bet.builder()
                .id(20L)
                .status(BetStatus.PENDING)
                .parentBet(parlay)
                .eventDate(now.minusHours(12))
                .build();
        Bet wonSibling = Bet.builder()
                .id(21L)
                .status(BetStatus.WON)
                .parentBet(parlay)
                .build();
        parlay.setChildBets(new LinkedHashSet<>(Set.of(blockingLeg, wonSibling)));

        Bet other = Bet.builder()
                .id(30L)
                .eventDate(now.minusHours(12))
                .build();

        List<Bet> sorted = prioritizer.sortByPriority(
                List.of(other, blockingLeg),
                List.of(parlay));

        assertEquals(20L, sorted.get(0).getId());
        assertEquals(30L, sorted.get(1).getId());
    }
}
