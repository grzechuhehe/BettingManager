package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;

import java.util.Optional;

public interface MarketResolver {

    boolean supports(Bet bet);

    Optional<BetStatus> resolve(Bet bet, SofaScoreEventDto event);
}
