package com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class OddsResponseDto {
    private String id;
    
    @JsonProperty("sport_key")
    private String sportKey;
    
    @JsonProperty("sport_title")
    private String sportTitle;
    
    @JsonProperty("commence_time")
    private String commenceTime;
    
    @JsonProperty("home_team")
    private String homeTeam;
    
    @JsonProperty("away_team")
    private String awayTeam;
    
    private List<BookmakerDto> bookmakers;

    @Data
    public static class BookmakerDto {
        private String key;
        private String title;
        
        @JsonProperty("last_update")
        private String lastUpdate;
        
        private List<MarketDto> markets;
    }

    @Data
    public static class MarketDto {
        private String key;
        
        @JsonProperty("last_update")
        private String lastUpdate;
        
        private List<OutcomeDto> outcomes;
    }

    @Data
    public static class OutcomeDto {
        private String name;
        private Double price;
        private Double point; // Opcjonalne dla handicapów/overów
    }
}
