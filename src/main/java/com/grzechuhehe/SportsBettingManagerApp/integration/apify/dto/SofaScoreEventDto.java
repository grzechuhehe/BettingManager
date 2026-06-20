package com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SofaScoreEventDto {
    private String type;          // "match"
    private String sport;         // "football"
    private String name;          // "Home - Away"
    private String homeTeam;
    private String awayTeam;
    private Integer homeScore;
    private Integer awayScore;
    private String statusType;    // "finished" / "canceled" / "postponed" / ...
    private Integer winnerCode;   // 1=home, 2=away, 3=draw
    private Long startTimestamp;  // epoch seconds
    private String tournament;
    private String url;
}
