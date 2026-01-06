package com.grzechuhehe.SportsBettingManagerApp.dto;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.OddsType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BetRequest {

    @NotNull(message = "Stake cannot be null")
    @Positive(message = "Stake must be positive")
    private BigDecimal stake;

    @NotNull(message = "Odds cannot be null")
    @DecimalMin(value = "1.01", message = "Odds must be greater than 1.00")
    private BigDecimal odds;

    private OddsType oddsType; // Może być null, jeśli domyślny DECIMAL

    @NotNull(message = "Sport cannot be null")
    private String sport;

    @NotNull(message = "Event name cannot be null")
    private String eventName;

    @NotNull(message = "Event date cannot be null")
    private LocalDateTime eventDate;

    @NotNull(message = "Market type cannot be null")
    private MarketType marketType;

    @NotNull(message = "Selection cannot be null")
    private String selection;

    private String line; // Opcjonalne dla handicapów/over_under

    @NotNull(message = "Bookmaker cannot be null")
    private String bookmaker;

    private String externalBetId; // Opcjonalne
    private String externalApiName; // Opcjonalne
    private String notes; // Opcjonalne
}
