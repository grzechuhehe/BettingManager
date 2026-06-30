package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.importing.MarketTypeInferrer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class BetResolutionEligibilityEvaluator {

    private final ResolutionNameTranslator nameTranslator;
    private final SelectionResolvabilityChecker selectionResolvabilityChecker;
    private final MarketTypeInferrer marketTypeInferrer;

    @Value("${bet.resolution.search-cooldown-hours:24}")
    private int searchCooldownHours;
    @Value("${bet.resolution.min-hours-after-placed:3}")
    private int minHoursAfterPlaced;

    public boolean isEligible(Bet bet, LocalDateTime now, boolean force) {
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
                : marketTypeInferrer.infer(bet);
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
