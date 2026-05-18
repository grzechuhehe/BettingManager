package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.grzechuhehe.SportsBettingManagerApp.model.EvOpportunity;
import com.grzechuhehe.SportsBettingManagerApp.repository.EvOpportunityRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.EvScannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ev/opportunities")
@RequiredArgsConstructor
@Tag(name = "EV Opportunities", description = "Endpoints for viewing automated +EV market opportunities")
public class EvOpportunityController {
    private final EvOpportunityRepository repository;
    private final com.grzechuhehe.SportsBettingManagerApp.service.EvScannerService evScannerService;

    @GetMapping
    @Operation(summary = "Get active EV opportunities", description = "Returns latest unique +EV opportunities detected in the last 2 hours")
    public List<EvOpportunity> getOpportunities() {
        // Only return the latest scan for each unique (event, selection, bookmaker)
        // Historical data stays in DB for ROI/Charts.
        return repository.findLatestUniqueOpportunities(LocalDateTime.now().minusHours(2));
    }


    @PostMapping("/trigger-scan")
    @Operation(summary = "Trigger manual scan", description = "Manually starts the automated +EV market scan process")
    public String triggerScan() {
        evScannerService.scanForEvOpportunities();
        return "Scan triggered successfully. Check logs for details.";
    }
}
