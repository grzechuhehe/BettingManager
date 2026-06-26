package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.market.MarketResolverRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BetOutcomeEvaluator {

    private final MarketResolverRegistry marketResolverRegistry;

    public Optional<BetStatus> evaluate(Bet bet, SofaScoreEventDto event) {
        if (event == null || bet.getMarketType() == null) {
            return Optional.empty();
        }
        String status = event.getStatusType() == null ? "" : event.getStatusType().toLowerCase(Locale.ROOT);
        if (status.equals("canceled") || status.equals("cancelled") || status.equals("postponed")) {
            return Optional.of(BetStatus.VOID);
        }
        if (!status.equals("finished") || event.getHomeScore() == null || event.getAwayScore() == null) {
            return Optional.empty();
        }
        return marketResolverRegistry.resolve(bet, event);
    }
}
