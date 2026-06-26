package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.market.BetBuilderMarketResolver;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.market.CompositeSelectionParser;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.market.HandicapMarketResolver;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SelectionResolvabilityChecker {

    private static final Pattern HANDICAP_IN_SELECTION =
            Pattern.compile(".*\\([+-]?\\d+(?:[.,]\\d+)?\\).*");

    private final BetBuilderMarketResolver betBuilderMarketResolver;
    private final HandicapMarketResolver handicapMarketResolver;

    public SelectionResolvabilityChecker(
            BetBuilderMarketResolver betBuilderMarketResolver,
            HandicapMarketResolver handicapMarketResolver) {
        this.betBuilderMarketResolver = betBuilderMarketResolver;
        this.handicapMarketResolver = handicapMarketResolver;
    }

    public boolean isAutoResolvable(Bet bet) {
        if (betBuilderMarketResolver.supports(bet)) {
            return true;
        }

        String selection = bet.getSelection() == null ? "" : bet.getSelection().toLowerCase(Locale.ROOT);
        if (selection.contains("bet builder") || selection.contains("betbuilder")) {
            return false;
        }

        if (HANDICAP_IN_SELECTION.matcher(selection).matches()) {
            return handicapMarketResolver.supports(bet);
        }
        return true;
    }
}
