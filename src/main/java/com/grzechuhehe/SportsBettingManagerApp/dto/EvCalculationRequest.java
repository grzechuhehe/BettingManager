package com.grzechuhehe.SportsBettingManagerApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvCalculationRequest {
    private String eventQuery;
    private BigDecimal bookmakerOdds;
}
