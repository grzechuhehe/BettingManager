package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.OddsApiClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.dto.OddsResponseDto;
import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.dto.SportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/odds")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Pozwalamy frontendowi na dostęp
public class OddsController {

    private final OddsApiClient oddsApiClient;

    @GetMapping("/sports")
    public List<SportDto> getSports() {
        return oddsApiClient.getSports();
    }

    @GetMapping("/markets/{sportKey}")
    public List<OddsResponseDto> getOdds(
            @PathVariable String sportKey,
            @RequestParam(defaultValue = "eu") String regions,
            @RequestParam(defaultValue = "h2h") String markets) {
        return oddsApiClient.getOdds(sportKey, regions, markets);
    }
}
