package com.grzechuhehe.SportsBettingManagerApp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateBetRequest {
    @NotEmpty(message = "Bet requests cannot be empty")
    @Size(min = 1, message = "At least one bet request is required")
    @Valid // Ważne, aby walidować również obiekty BetRequest w liście
    private List<BetRequest> bets;
}
