package com.grzechuhehe.SportsBettingManagerApp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request object for creating one or more bets. If multiple bets are provided, they are treated as a parlay (accumulator).",
        example = "{\"bets\": [{\"stake\": 10.0, \"odds\": 1.95, \"sport\": \"Soccer\", \"eventName\": \"Team A vs Team B\", \"eventDate\": \"2024-06-01T20:00:00\", \"marketType\": \"H2H\", \"selection\": \"Team A\", \"bookmaker\": \"Bet365\"}, {\"stake\": 10.0, \"odds\": 2.10, \"sport\": \"Basketball\", \"eventName\": \"Team C vs Team D\", \"eventDate\": \"2024-06-01T22:00:00\", \"marketType\": \"H2H\", \"selection\": \"Team C\", \"bookmaker\": \"Bet365\"}]}")
public class CreateBetRequest {
    @NotEmpty(message = "Bet requests cannot be empty")
    @Size(min = 1, message = "At least one bet request is required")
    @Valid // Ważne, aby walidować również obiekty BetRequest w liście
    @Schema(description = "List of bet details. For a single bet, provide one item. For a parlay, provide multiple items.")
    private List<BetRequest> bets;
}
