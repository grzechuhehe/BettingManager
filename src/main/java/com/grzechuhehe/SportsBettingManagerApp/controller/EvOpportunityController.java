package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.grzechuhehe.SportsBettingManagerApp.model.EvOpportunity;
import com.grzechuhehe.SportsBettingManagerApp.repository.EvOpportunityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ev/opportunities")
@RequiredArgsConstructor
@Tag(name = "EV Opportunities", description = "Endpoints for viewing automated +EV market opportunities")
public class EvOpportunityController {
    private final EvOpportunityRepository repository;

    @GetMapping
    @Operation(summary = "Get all EV opportunities", description = "Returns a list of all currently detected +EV opportunities from the automated scanner")
    public List<EvOpportunity> getOpportunities() {
        return repository.findAll();
    }
}
