package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.grzechuhehe.SportsBettingManagerApp.dto.EvCalculationRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.EvCalculationResponse;
import com.grzechuhehe.SportsBettingManagerApp.service.EvCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ev")
@RequiredArgsConstructor
@Tag(name = "Expected Value", description = "Endpoints for calculating +EV against predictive markets")
public class EvController {

    private final EvCalculationService evCalculationService;

    @PostMapping("/calculate")
    @Operation(summary = "Calculate EV", description = "Compares bookmaker odds against predictive market probability")
    public ResponseEntity<EvCalculationResponse> calculateEv(@RequestBody EvCalculationRequest request) {
        EvCalculationResponse response = evCalculationService.calculateExpectedValue(request);
        return ResponseEntity.ok(response);
    }
}
