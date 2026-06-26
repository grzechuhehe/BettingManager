package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MarketResolverRegistry {

    private final List<MarketResolver> resolvers;

    public MarketResolverRegistry(
            BetBuilderMarketResolver betBuilderMarketResolver,
            HandicapMarketResolver handicapMarketResolver,
            StandardMarketResolver standardMarketResolver) {
        this.resolvers = List.of(betBuilderMarketResolver, handicapMarketResolver, standardMarketResolver);
    }

    public Optional<BetStatus> resolve(Bet bet, SofaScoreEventDto event) {
        for (MarketResolver resolver : resolvers) {
            if (resolver.supports(bet)) {
                return resolver.resolve(bet, event);
            }
        }
        return Optional.empty();
    }
}
