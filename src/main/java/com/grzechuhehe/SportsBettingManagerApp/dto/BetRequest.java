package com.grzechuhehe.SportsBettingManagerApp.dto;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.OddsType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Details of a single bet selection")
public class BetRequest {

    @NotNull(message = "Stake cannot be null")
    @Positive(message = "Stake must be positive")
    @DecimalMax(value = "1000000.00", message = "Stake exceeds maximum allowed amount ($1,000,000)")
    @Schema(description = "The amount of money wagered", example = "10.00")
    private BigDecimal stake;

    @NotNull(message = "Odds cannot be null")
    @DecimalMin(value = "1.01", message = "Odds must be greater than 1.00")
    @DecimalMax(value = "10000.00", message = "Odds exceed realistic bounds (max 10000.00)")
    @Schema(description = "The odds for the selection", example = "1.95")
    private BigDecimal odds;

    @Schema(description = "The format of the odds (e.g., DECIMAL, AMERICAN)", defaultValue = "DECIMAL")
    private OddsType oddsType; // Może być null, jeśli domyślny DECIMAL

    @NotNull(message = "Sport cannot be null")
    @Schema(description = "The sport category", example = "Soccer")
    private String sport;

    @NotNull(message = "Event name cannot be null")
    @Size(max = 255, message = "Event name is too long")
    @Schema(description = "The name of the event", example = "Real Madrid vs Barcelona")
    private String eventName;

    @NotNull(message = "Event date cannot be null")
    @Schema(description = "The scheduled date and time of the event")
    private LocalDateTime eventDate;

    @NotNull(message = "Market type cannot be null")
    @Schema(description = "The type of market (e.g., H2H, OVER_UNDER)")
    private MarketType marketType;

    @NotNull(message = "Selection cannot be null")
    @Schema(description = "The specific outcome selected", example = "Real Madrid")
    private String selection;

    @Schema(description = "The line or spread for the selection (if applicable)", example = "-1.5")
    private String line; // Opcjonalne dla handicapów/over_under

    @NotNull(message = "Bookmaker cannot be null")
    @Schema(description = "The name of the bookmaker where the bet was placed", example = "Bet365")
    private String bookmaker;

    @Schema(description = "Optional external ID from a bookmaker or API")
    private String externalBetId; // Opcjonalne
    @Schema(description = "Optional name of the external API source")
    private String externalApiName; // Opcjonalne
    @Schema(description = "Optional personal notes about the bet")
    private String notes; // Opcjonalne
}
