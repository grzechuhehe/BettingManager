package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.grzechuhehe.SportsBettingManagerApp.dto.BasicStatsDTO;
import com.grzechuhehe.SportsBettingManagerApp.dto.BetResponse;
import com.grzechuhehe.SportsBettingManagerApp.dto.BetStatistics;
import com.grzechuhehe.SportsBettingManagerApp.dto.CreateBetRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.SettleBetRequest;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetResolutionAttemptRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.BettingService;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.BetResolutionService;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.AutoResolutionGuard;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionCycleMetricsHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.grzechuhehe.SportsBettingManagerApp.service.ImageStorageService;
import com.grzechuhehe.SportsBettingManagerApp.dto.DashboardStatsDTO;
import com.grzechuhehe.SportsBettingManagerApp.dto.HeatmapResponse;
import com.grzechuhehe.SportsBettingManagerApp.dto.BetRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
@Tag(name = "Bet Management", description = "Endpoints for managing user bets, including single and parlay bets")
public class BetController {
    private final BettingService bettingService;
    private final BetResolutionService betResolutionService;
    private final BetResolutionAttemptRepository resolutionAttemptRepository;
    private final ResolutionCycleMetricsHolder resolutionMetricsHolder;
    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final ImageStorageService imageStorageService;

    @Value("${bet.resolution.debug-endpoints:false}")
    private boolean debugEndpoints;

    @Value("${bet.resolution.oldest-one-shot.cutoff:2026-06-25T00:00:00}")
    private String oldestOneShotCutoff;

    @Value("${bet.resolution.oldest-one-shot.limit:80}")
    private int oldestOneShotLimit;

    @Value("${bet.resolution.oldest-one-shot.date-window-days:30}")
    private int oldestOneShotDateWindowDays;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BetController.class);

    @PostMapping("/add-bet")
    @Operation(summary = "Create a new bet", description = "Adds a new single bet or a parlay (if multiple selections are provided) to the user's portfolio")
    public ResponseEntity<List<BetResponse>> createBet(@Valid @RequestBody CreateBetRequest createBetRequest) {
        logger.info("Otrzymano żądanie utworzenia zakładu: {}", createBetRequest);

        // Pobierz aktualnie uwierzytelnionego użytkownika
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Zapisuję zakład dla użytkownika: {}", username);

        List<Bet> placedBets = bettingService.placeBet(createBetRequest, username);
        List<BetResponse> betResponses = placedBets.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        logger.info("Zakład(y) zapisane pomyślnie. Liczba zakładów: {}", placedBets.size());
        return ResponseEntity.ok(betResponses);
    }

    @PostMapping(value = "/add-bet-with-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a bet with optional screenshot proof",
            description = "Same as add-bet but accepts an optional bet slip image stored as evidence.")
    public ResponseEntity<?> createBetWithProof(
            @RequestPart("request") @Valid CreateBetRequest createBetRequest,
            @RequestPart(value = "proof", required = false) MultipartFile proof) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (proof != null && !proof.isEmpty()) {
            try {
                String imagePath = imageStorageService.saveUploadedImage(proof, "manual/" + username);
                createBetRequest.setImageProofPath(imagePath);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "invalid_file",
                        "message", e.getMessage()));
            }
        }
        List<Bet> placedBets = bettingService.placeBet(createBetRequest, username);
        List<BetResponse> betResponses = placedBets.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(betResponses);
    }

    @GetMapping
    @Operation(summary = "Get all bets", description = "Retrieves all bets placed by the currently authenticated user")
    public ResponseEntity<List<BetResponse>> getAllBetsForCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Bet> bets = bettingService.getUserBets(currentUser);
        List<BetResponse> betResponses = bets.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        logger.info("Pobrano {} zakładów dla użytkownika: {}", betResponses.size(), username);
        return ResponseEntity.ok(betResponses);
    }


    @GetMapping("/dashboard-stats")
    @Operation(summary = "Get dashboard statistics", description = "Retrieves comprehensive statistics for the user's dashboard, including profit/loss, ROI, yield, and equity curve")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(bettingService.getDashboardStats(currentUser));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get basic statistics", description = "Retrieves basic betting statistics including total bets, won bets, total stake, profit/loss, ROI, and recent bets")
    public ResponseEntity<BasicStatsDTO> getStats() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Map<String, Object> rawStats = bettingService.getStatistics(currentUser);
        
        @SuppressWarnings("unchecked")
        List<Bet> recentBetsRaw = (List<Bet>) rawStats.get("recentBets");
        List<BetResponse> recentBets = recentBetsRaw.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        BasicStatsDTO statsDTO = new BasicStatsDTO(
            (Integer) rawStats.get("totalBets"),
            (Long) rawStats.get("wonBets"),
            (BigDecimal) rawStats.get("totalStake"),
            (BigDecimal) rawStats.get("profitLoss"),
            (BigDecimal) rawStats.get("roi"),
            recentBets,
            (String) rawStats.get("displayCurrency")
        );
        
        return ResponseEntity.ok(statsDTO);
    }

    @GetMapping("/advanced-stats")
    @Operation(summary = "Get advanced statistics", description = "Retrieves advanced betting statistics including win rates by type, rolling average, streaks, and Sharpe ratio")
    public ResponseEntity<BetStatistics> getAdvancedStats() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(bettingService.getAdvancedStatistics(currentUser));
    }

    @GetMapping("/heatmap")
    @Operation(summary = "Get heatmap data", description = "Retrieves daily profit/loss data for generating a betting heatmap")
    public ResponseEntity<HeatmapResponse> getHeatmapData() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(bettingService.getHeatmapData(currentUser));
    }


    @GetMapping("/{id}/resolution-attempts")
    @Operation(summary = "List recent resolution attempts for a bet (debug, dev profile only)")
    public ResponseEntity<?> getResolutionAttempts(@PathVariable Long id) {
        if (!debugEndpoints) {
            return ResponseEntity.notFound().build();
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Bet bet = betRepository.findById(id).orElse(null);
        if (bet == null || bet.getUser() == null
                || !bet.getUser().getId().equals(currentUser.getId())) {
            // 404 zamiast 403, by nie ujawniać istnienia cudzego zasobu
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(resolutionAttemptRepository.findTop10ByBetIdOrderByAttemptedAtDesc(id));
    }

    @GetMapping("/resolution/metrics/last-cycle")
    @Operation(summary = "Last resolution cycle metrics (debug, when bet.resolution.debug-endpoints=true)")
    public ResponseEntity<?> getLastResolutionCycleMetrics() {
        if (!debugEndpoints) {
            return ResponseEntity.notFound().build();
        }
        return resolutionMetricsHolder.getLast()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/run-auto-resolution")
    @Operation(summary = "Trigger auto-resolution", description = "Starts one cycle of automatic bet settlement from SofaScore (Apify). Runs in background (~2–5 min). Use force=true to ignore cooldown.")
    public ResponseEntity<Map<String, String>> runAutoResolution(
            @RequestParam(defaultValue = "false") boolean force) {
        AutoResolutionGuard.AcquireResult acquire = betResolutionService.triggerManualResolution(force);
        int cooldownMinutes = betResolutionService.getManualCooldownMinutes();
        return switch (acquire.status()) {
            case COOLDOWN -> ResponseEntity.status(429).body(Map.of(
                    "status", "cooldown",
                    "message", "Auto-resolution was run recently. Try again in " + acquire.remainingMinutes() + " min.",
                    "retryAfterMinutes", String.valueOf(acquire.remainingMinutes())));
            case BUSY -> ResponseEntity.status(409).body(Map.of(
                    "status", "busy",
                    "message", "Auto-resolution is already running"));
            case ACQUIRED -> ResponseEntity.accepted().body(Map.of(
                    "status", "started",
                    "message", "Auto-resolution started in background (force=" + force + "). Refresh in 2–5 min.",
                    "retryAfterMinutes", String.valueOf(cooldownMinutes)));
        };
    }

    @PostMapping("/run-oldest-pending-one-shot")
    @Operation(summary = "One-shot oldest pending resolution (debug)",
            description = "Resolves up to N oldest PENDING roots with placedAt before cutoff. Requires bet.resolution.debug-endpoints=true.")
    public ResponseEntity<Map<String, String>> runOldestPendingOneShot(
            @RequestParam(defaultValue = "true") boolean force) {
        if (!debugEndpoints) {
            return ResponseEntity.notFound().build();
        }
        LocalDateTime cutoff = LocalDateTime.parse(oldestOneShotCutoff);
        AutoResolutionGuard.AcquireResult acquire = betResolutionService.triggerOldestPendingOneShot(
                cutoff, oldestOneShotLimit, oldestOneShotDateWindowDays, force);
        return switch (acquire.status()) {
            case COOLDOWN -> ResponseEntity.status(429).body(Map.of(
                    "status", "cooldown",
                    "message", "Resolution guard cooldown. Retry in " + acquire.remainingMinutes() + " min."));
            case BUSY -> ResponseEntity.status(409).body(Map.of(
                    "status", "busy",
                    "message", "Auto-resolution is already running"));
            case ACQUIRED -> ResponseEntity.accepted().body(Map.of(
                    "status", "started",
                    "message", "Oldest-pending one-shot started. cutoff=" + cutoff + ", limit=" + oldestOneShotLimit));
        };
    }

    @PatchMapping("/{id}/settle")
    @Operation(summary = "Settle a bet", description = "Updates the status of a bet (e.g., WON, LOST, VOID) and calculates final profit")
    public ResponseEntity<BetResponse> settleBet(@PathVariable Long id, @Valid @RequestBody SettleBetRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Bet settledBet = bettingService.settleBet(id, request.status(), currentUser);
        return ResponseEntity.ok(convertToDto(settledBet));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a bet", description = "Partially updates an existing bet; missing fields keep their current value")
    public ResponseEntity<BetResponse> updateBet(@PathVariable Long id, @RequestBody BetRequest betRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Bet updatedBet = bettingService.updateBet(id, betRequest, currentUser);
        return ResponseEntity.ok(convertToDto(updatedBet));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a bet", description = "Removes a bet from the system")
    public ResponseEntity<Void> deleteBet(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        bettingService.deleteBet(id, currentUser);
        return ResponseEntity.ok().build();
    }

    // Metoda pomocnicza do konwersji Bet na BetResponse
    private BetResponse convertToDto(Bet bet) {
        BetResponse dto = new BetResponse();
        dto.setId(bet.getId());
        dto.setBetType(bet.getBetType());
        dto.setStatus(bet.getStatus());
        dto.setStake(bet.getStake());
        dto.setCurrency(bet.getCurrency() != null ? bet.getCurrency() : "PLN");
        dto.setOdds(bet.getOdds());
        dto.setOddsType(bet.getOddsType());
        dto.setPotentialWinnings(bet.getPotentialWinnings());
        dto.setFinalProfit(bet.getFinalProfit());
        dto.setSport(bet.getSport());
        dto.setEventName(bet.getEventName());
        dto.setEventDate(bet.getEventDate());
        dto.setMarketType(bet.getMarketType());
        dto.setSelection(bet.getSelection());
        dto.setLine(bet.getLine());
        dto.setBookmaker(bet.getBookmaker());
        dto.setExternalBetId(bet.getExternalBetId());
        dto.setExternalApiName(bet.getExternalApiName());
        dto.setPlacedAt(bet.getPlacedAt());
        dto.setSettledAt(bet.getSettledAt());
        dto.setNotes(bet.getNotes());
        dto.setImageProofPath(bet.getImageProofPath());
        if (bet.getUser() != null) {
            dto.setUserId(bet.getUser().getId());
        }
        if (bet.getChildBets() != null && !bet.getChildBets().isEmpty()) {
            dto.setChildBets(bet.getChildBets().stream()
                                .map(this::convertToDto)
                                .collect(Collectors.toList()));
        }
        return dto;
    }
}
