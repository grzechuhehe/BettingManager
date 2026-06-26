package com.grzechuhehe.SportsBettingManagerApp.dto;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;

import java.math.BigDecimal;

public record ImportedBetResponse(
        Long id,
        String eventName,
        String selection,
        BigDecimal odds,
        BigDecimal stake,
        String sport,
        MarketType marketType,
        BetStatus status,
        BetType betType,
        String imageProofPath,
        int legsCount
) {
    public static ImportedBetResponse from(Bet bet) {
        return new ImportedBetResponse(
                bet.getId(),
                bet.getEventName(),
                bet.getSelection(),
                bet.getOdds(),
                bet.getStake(),
                bet.getSport(),
                bet.getMarketType(),
                bet.getStatus(),
                bet.getBetType(),
                bet.getImageProofPath(),
                bet.getChildBets() == null ? 0 : bet.getChildBets().size()
        );
    }
}
