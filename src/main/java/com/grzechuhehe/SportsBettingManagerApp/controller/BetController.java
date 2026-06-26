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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import com.grzechuhehe.SportsBettingManagerApp.dto.DashboardStatsDTO;
import com.grzechuhehe.SportsBettingManagerApp.dto.BetRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
@Tag(name = "Bet Management", description = "Endpoints for managing user bets, including single and parlay bets")
public class BetController {
    private final BettingService bettingService;
    private final BetResolutionService betResolutionService;
    private final BetResolutionAttemptRepository resolutionAttemptRepository;
    private final UserRepository userRepository;
    private final BetRepository betRepository;

    @Value("${bet.resolution.debug-endpoints:false}")
    private boolean debugEndpoints;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BetController.class);
    private static final AtomicBoolean RESOLUTION_RUNNING =
            new AtomicBoolean(false);

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
            recentBets
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
    public ResponseEntity<Map<String, BigDecimal>> getHeatmapData() {
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

    @PostMapping("/run-auto-resolution")
    @Operation(summary = "Trigger auto-resolution", description = "Starts one cycle of automatic bet settlement from SofaScore (Apify). Runs in background (~2–5 min). Use force=true to ignore cooldown.")
    public ResponseEntity<Map<String, String>> runAutoResolution(
            @RequestParam(defaultValue = "true") boolean force) {
        if (!RESOLUTION_RUNNING.compareAndSet(false, true)) {
            return ResponseEntity.status(409).body(Map.of(
                    "status", "busy",
                    "message", "Auto-resolution is already running"));
        }
        Thread.startVirtualThread(() -> {
            try {
                logger.info("Manual auto-resolution started (force={})", force);
                betResolutionService.resolvePendingBets(force);
                logger.info("Manual auto-resolution finished (force={})", force);
            } catch (Exception e) {
                logger.error("Manual auto-resolution failed: {}", e.getMessage(), e);
            } finally {
                RESOLUTION_RUNNING.set(false);
            }
        });
        return ResponseEntity.accepted().body(Map.of(
                "status", "started",
                "message", "Auto-resolution started in background (force=" + force + "). Refresh in 2–5 min."));
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

    @PutMapping("/{id}")
    @Operation(summary = "Update a bet", description = "Updates the details of an existing bet")
    public ResponseEntity<BetResponse> updateBet(@PathVariable Long id, @Valid @RequestBody BetRequest betRequest) {
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
