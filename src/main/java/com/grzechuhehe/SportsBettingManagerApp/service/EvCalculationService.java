package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.dto.EvCalculationRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.EvCalculationResponse;
import com.grzechuhehe.SportsBettingManagerApp.integration.polymarket.PolymarketApiClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class EvCalculationService {

    private final PolymarketApiClient polymarketApiClient;

    public EvCalculationService(PolymarketApiClient polymarketApiClient) {
        this.polymarketApiClient = polymarketApiClient;
    }

    public EvCalculationResponse calculateExpectedValue(EvCalculationRequest request) {
        BigDecimal trueProbability = polymarketApiClient.fetchTrueProbability(request.getEventQuery())
                .orElse(new BigDecimal("0.5000"));
        
        // EV Formula: (Decimal Odds * True Probability) - 1.0
        BigDecimal expectedValue = request.getBookmakerOdds()
                .multiply(trueProbability)
                .subtract(BigDecimal.ONE);
                
        BigDecimal expectedValuePercentage = expectedValue
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
                
        boolean isPositive = expectedValuePercentage.compareTo(BigDecimal.ZERO) > 0;

        return new EvCalculationResponse(
                request.getEventQuery(),
                request.getBookmakerOdds(),
                trueProbability,
                expectedValuePercentage,
                isPositive
        );
    }
}
