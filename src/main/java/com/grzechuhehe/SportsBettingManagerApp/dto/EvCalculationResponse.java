package com.grzechuhehe.SportsBettingManagerApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvCalculationResponse {
    private String eventName;
    private BigDecimal bookmakerOdds;
    private BigDecimal trueProbability;
    private BigDecimal expectedValuePercentage;
    private boolean isPositiveEv;
}
