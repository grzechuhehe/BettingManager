package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.OddsApiClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.dto.OddsResponseDto;
import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.dto.SportDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/odds")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Pozwalamy frontendowi na dostęp
@Tag(name = "Live Markets", description = "Endpoints for fetching live sports odds and markets")
public class OddsController {

    private final OddsApiClient oddsApiClient;

    @GetMapping("/sports")
    @Operation(summary = "Get available sports", description = "Retrieves a list of all sports currently available for betting")
    public List<SportDto> getSports() {
        return oddsApiClient.getSports();
    }

    @GetMapping("/markets/{sportKey}")
    @Operation(summary = "Get live odds", description = "Retrieves live odds for a specific sport and markets")
    public List<OddsResponseDto> getOdds(
            @PathVariable String sportKey,
            @RequestParam(defaultValue = "eu") String regions,
            @RequestParam(defaultValue = "h2h") String markets) {
        return oddsApiClient.getOdds(sportKey, regions, markets);
    }
}
