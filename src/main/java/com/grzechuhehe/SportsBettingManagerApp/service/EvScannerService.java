package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.dto.UnifiedMarketData;
import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.OddsApiClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.dto.OddsResponseDto;
import com.grzechuhehe.SportsBettingManagerApp.model.EvOpportunity;
import com.grzechuhehe.SportsBettingManagerApp.repository.EvOpportunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvScannerService {
    private final OddsApiClient oddsClient;
    private final PredictionMarketService predictionMarketService;
    private final EvOpportunityRepository repository;

    @Scheduled(fixedRate = 3600000, initialDelay = 5000) // Scan hourly, start after 5s
    public void scanForEvOpportunities() {
        log.info("Starting automated +EV market scan...");
        try {
            // We no longer deleteAll() here to preserve history for future ROI simulations

            // For MVP, we scan EPL soccer as a high-liquidity market
            List<OddsResponseDto> oddsResponses = oddsClient.getOdds("soccer_epl", "eu", "h2h");
            log.info("Fetched {} events from TheOddsAPI", oddsResponses != null ? oddsResponses.size() : 0);
            
            if (oddsResponses == null || oddsResponses.isEmpty()) {
                log.warn("No odds fetched from TheOddsAPI. Check your API key or sport key.");
                return;
            }

            for (OddsResponseDto event : oddsResponses) {
                String eventName = event.getHomeTeam() + " vs " + event.getAwayTeam();
                
                // Fetch Map of UnifiedMarketData
                java.util.Map<String, UnifiedMarketData> unifiedData = predictionMarketService.getUnifiedProbabilities(event.getHomeTeam(), event.getAwayTeam());
                
                if (unifiedData.isEmpty()) {
                    log.debug("No prediction market match found for: {}", eventName);
                    continue;
                }

                for (OddsResponseDto.BookmakerDto bookmaker : event.getBookmakers()) {
                    for (OddsResponseDto.MarketDto market : bookmaker.getMarkets()) {
                        for (OddsResponseDto.OutcomeDto outcome : market.getOutcomes()) {
                            
                            // Find matching data in the map
                            UnifiedMarketData bestData = null;
                            
                            for (java.util.Map.Entry<String, UnifiedMarketData> entry : unifiedData.entrySet()) {
                                if (outcome.getName().toLowerCase().contains(entry.getKey().toLowerCase().split(" ")[0])) {
                                    bestData = entry.getValue();
                                    break;
                                }
                            }
                            
                            if (bestData == null || bestData.blendedProbability().compareTo(BigDecimal.ZERO) <= 0 || bestData.blendedProbability().compareTo(BigDecimal.ONE) >= 0) {
                                // Skip if no probability or if it's 100%
                                continue;
                            }

                            BigDecimal trueProbability = bestData.blendedProbability();
                            BigDecimal marketLiquidity = bestData.totalOpenInterest();
                            BigDecimal bookmakerOdds = BigDecimal.valueOf(outcome.getPrice());
                            
                            // EV = (Odds * Prob) - 1
                            BigDecimal ev = bookmakerOdds.multiply(trueProbability).subtract(BigDecimal.ONE);
                            BigDecimal evPercentage = ev.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
                            
                            // Only save if EV is positive AND probability is realistic AND EV >= 2%
                            if (ev.compareTo(BigDecimal.ZERO) > 0 && evPercentage.compareTo(BigDecimal.valueOf(2)) >= 0 && evPercentage.compareTo(BigDecimal.valueOf(1000)) < 0) {
                                log.info("Found +EV Opportunity: {} | {} | {}% EV | OI: ${} | Sources: {}", 
                                        eventName, outcome.getName(), evPercentage, marketLiquidity, bestData.sources());
                                
                                EvOpportunity opp = new EvOpportunity();
                                opp.setEventName(eventName); 
                                opp.setTargetSelection(outcome.getName()); 
                                opp.setBookmaker(bookmaker.getTitle());
                                opp.setBookmakerOdds(bookmakerOdds);
                                opp.setTrueProbability(trueProbability);
                                opp.setEvPercentage(evPercentage);
                                opp.setMarketLiquidity(marketLiquidity);
                                opp.setSources(String.join(",", bestData.sources()));
                                opp.setDetectedAt(LocalDateTime.now());
                                
                                repository.save(opp);
                            }
                        }
                    }
                }
                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            log.error("EV scanning interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error during EV scanning: {}", e.getMessage());
        }
        log.info("EV market scan completed.");
    }
}
