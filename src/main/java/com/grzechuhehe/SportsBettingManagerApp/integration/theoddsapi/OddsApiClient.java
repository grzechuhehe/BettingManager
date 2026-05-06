package com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi;

import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.dto.OddsResponseDto;
import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.dto.SportDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class OddsApiClient {
    private static final Logger logger = LoggerFactory.getLogger(OddsApiClient.class);
    
    private final RestClient restClient;
    private final String apiKey;
    private final String baseUrl;

    public OddsApiClient(
            @Value("${odds.api.key:dummy-key}") String apiKey,
            @Value("${odds.api.base-url:https://api.the-odds-api.com/v4}") String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Pobiera listę dostępnych sportów i lig.
     */
    public List<SportDto> getSports() {
        logger.info("Fetching sports from The-Odds-API");
        
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/sports")
                        .queryParam("apiKey", apiKey)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<SportDto>>() {});
    }

    /**
     * Pobiera kursy dla konkretnej ligi i regionu.
     * Regiony: eu, us, uk, au
     * Markety: h2h (mecz), spreads (handicap), totals (over/under)
     */
    public List<OddsResponseDto> getOdds(String sportKey, String regions, String markets) {
        logger.info("Fetching odds for sport: {} in regions: {} with markets: {}", sportKey, regions, markets);
        
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/sports/{sport}/odds")
                        .queryParam("apiKey", apiKey)
                        .queryParam("regions", regions)
                        .queryParam("markets", markets)
                        .queryParam("oddsFormat", "decimal")
                        .build(sportKey))
                .retrieve()
                .body(new ParameterizedTypeReference<List<OddsResponseDto>>() {});
    }
}
