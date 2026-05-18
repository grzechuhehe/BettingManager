package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.OddsApiClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.polymarket.PolymarketApiClient;
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
    private final PolymarketApiClient polyClient;
    private final EvOpportunityRepository repository;

    @Scheduled(fixedRate = 3600000) // Scan hourly
    public void scanForEvOpportunities() {
        log.info("Starting automated +EV market scan...");
        try {
            // For MVP, we scan EPL soccer as a high-liquidity market
            List<OddsResponseDto> oddsResponses = oddsClient.getOdds("soccer_epl", "eu", "h2h");
            
            for (OddsResponseDto event : oddsResponses) {
                String eventName = event.getHomeTeam() + " vs " + event.getAwayTeam();
                
                for (OddsResponseDto.BookmakerDto bookmaker : event.getBookmakers()) {
                    for (OddsResponseDto.MarketDto market : bookmaker.getMarkets()) {
                        for (OddsResponseDto.OutcomeDto outcome : market.getOutcomes()) {
                            BigDecimal bookmakerOdds = BigDecimal.valueOf(outcome.getPrice());
                            
                            // Query Polymarket for this specific outcome
                            BigDecimal trueProbability = polyClient.fetchTrueProbability(outcome.getName());
                            
                            // EV = (Odds * Prob) - 1
                            BigDecimal ev = bookmakerOdds.multiply(trueProbability).subtract(BigDecimal.ONE);
                            BigDecimal evPercentage = ev.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
                            
                            if (ev.compareTo(BigDecimal.ZERO) > 0) {
                                log.info("Found +EV Opportunity: {} | {} | {}% EV", eventName, outcome.getName(), evPercentage);
                                
                                EvOpportunity opp = new EvOpportunity();
                                opp.setEventName(eventName + " (" + outcome.getName() + ")");
                                opp.setBookmaker(bookmaker.getTitle());
                                opp.setBookmakerOdds(bookmakerOdds);
                                opp.setTrueProbability(trueProbability);
                                opp.setEvPercentage(evPercentage);
                                opp.setDetectedAt(LocalDateTime.now());
                                
                                repository.save(opp);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during EV scanning: {}", e.getMessage());
        }
        log.info("EV market scan completed.");
    }
}
