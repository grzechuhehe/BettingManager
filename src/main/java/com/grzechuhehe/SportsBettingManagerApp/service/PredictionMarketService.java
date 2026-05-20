package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.dto.UnifiedMarketData;
import com.grzechuhehe.SportsBettingManagerApp.integration.kalshi.KalshiApiClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.polymarket.MarketData;
import com.grzechuhehe.SportsBettingManagerApp.integration.polymarket.PolymarketApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PredictionMarketService {
    private final PolymarketApiClient polyClient;
    private final KalshiApiClient kalshiClient;

    public Map<String, UnifiedMarketData> getUnifiedProbabilities(String homeTeam, String awayTeam) {
        Map<String, MarketData> poly = polyClient.fetchMarketProbabilities(homeTeam, awayTeam);
        Map<String, MarketData> kalshi = kalshiClient.fetchMarketProbabilities(homeTeam, awayTeam);

        Map<String, UnifiedMarketData> unified = new HashMap<>();
        
        // Get all unique outcomes (Home, Away, Draw)
        java.util.Set<String> outcomes = new java.util.HashSet<>(poly.keySet());
        outcomes.addAll(kalshi.keySet());

        for (String outcome : outcomes) {
            MarketData pData = poly.get(outcome);
            MarketData kData = kalshi.get(outcome);

            if (pData != null && kData != null) {
                // Option B: Smart Blending (Weighted Average by OI)
                BigDecimal totalOI = pData.openInterest().add(kData.openInterest());
                BigDecimal blendedProb;
                
                if (totalOI.compareTo(BigDecimal.ZERO) > 0) {
                    blendedProb = pData.probability().multiply(pData.openInterest())
                            .add(kData.probability().multiply(kData.openInterest()))
                            .divide(totalOI, 4, RoundingMode.HALF_UP);
                } else {
                    // Fallback to simple average if both have 0 OI
                    blendedProb = pData.probability().add(kData.probability())
                            .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
                }
                
                unified.put(outcome, new UnifiedMarketData(blendedProb, totalOI, List.of("POLYMARKET", "KALSHI")));
            } else if (pData != null) {
                unified.put(outcome, new UnifiedMarketData(pData.probability(), pData.openInterest(), List.of("POLYMARKET")));
            } else if (kData != null) {
                unified.put(outcome, new UnifiedMarketData(kData.probability(), kData.openInterest(), List.of("KALSHI")));
            }
        }
        return unified;
    }
}
