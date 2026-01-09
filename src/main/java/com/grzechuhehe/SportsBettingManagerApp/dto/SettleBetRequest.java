package com.grzechuhehe.SportsBettingManagerApp.dto;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import jakarta.validation.constraints.NotNull;

public record SettleBetRequest(
    @NotNull(message = "Status is required")
    BetStatus status
) {}
