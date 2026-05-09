package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.grzechuhehe.SportsBettingManagerApp.dto.BasicStatsDTO;
import com.grzechuhehe.SportsBettingManagerApp.dto.BetResponse;
import com.grzechuhehe.SportsBettingManagerApp.dto.BetStatistics;
import com.grzechuhehe.SportsBettingManagerApp.dto.CreateBetRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.SettleBetRequest;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.BettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
@Tag(name = "Bet Management", description = "Endpoints for managing user bets, including single and parlay bets")
public class BetController {
    private final BettingService bettingService;
    private final UserRepository userRepository;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BetController.class);

    @PostMapping("/add-bet")
    @Operation(summary = "Create a new bet", description = "Adds a new single bet or a parlay (if multiple selections are provided) to the user's portfolio")
    public ResponseEntity<?> createBet(@Valid @RequestBody CreateBetRequest createBetRequest, BindingResult result) {
        logger.info("Otrzymano żądanie utworzenia zakładu: {}", createBetRequest);

        if (result.hasErrors()) {
            logger.error("Błędy walidacji: {}", result.getFieldErrors());
            return ResponseEntity.badRequest().body(result.getFieldErrors());
        }

        try {
            // Pobierz aktualnie uwierzytelnionego użytkownika
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Zapisuję zakład dla użytkownika: {}", username);

            List<Bet> placedBets = bettingService.placeBet(createBetRequest, username);
            List<BetResponse> betResponses = placedBets.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            logger.info("Zakład(y) zapisane pomyślnie. Liczba zakładów: {}", placedBets.size());
            return ResponseEntity.ok(betResponses);
        } catch (IllegalArgumentException e) {
            logger.error("Błąd walidacji lub brak użytkownika: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Błąd podczas zapisywania zakładu: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping
    @Operation(summary = "Get all bets", description = "Retrieves all bets placed by the currently authenticated user")
    public ResponseEntity<List<BetResponse>> getAllBetsForCurrentUser() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            List<Bet> bets = bettingService.getUserBets(currentUser);
            List<BetResponse> betResponses = bets.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            logger.info("Pobrano {} zakładów dla użytkownika: {}", betResponses.size(), username);
            return ResponseEntity.ok(betResponses);
        } catch (Exception e) {
            logger.error("Błąd podczas pobierania zakładów dla bieżącego użytkownika: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }


    @GetMapping("/dashboard-stats")
    @Operation(summary = "Get dashboard statistics", description = "Retrieves comprehensive statistics for the user's dashboard, including profit/loss, ROI, yield, and equity curve")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            return ResponseEntity.ok(bettingService.getDashboardStats(currentUser));
        } catch (Exception e) {
            logger.error("Błąd podczas pobierania statystyk dashboardu: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get basic statistics", description = "Retrieves basic betting statistics including total bets, won bets, total stake, profit/loss, ROI, and recent bets")
    public ResponseEntity<BasicStatsDTO> getStats() {
        try {
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
        } catch (Exception e) {
            logger.error("Błąd podczas pobierania statystyk: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/advanced-stats")
    @Operation(summary = "Get advanced statistics", description = "Retrieves advanced betting statistics including win rates by type, rolling average, streaks, and Sharpe ratio")
    public ResponseEntity<BetStatistics> getAdvancedStats() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            return ResponseEntity.ok(bettingService.getAdvancedStatistics(currentUser));
        } catch (Exception e) {
            logger.error("Błąd podczas pobierania zaawansowanych statystyk: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/heatmap")
    @Operation(summary = "Get heatmap data", description = "Retrieves daily profit/loss data for generating a betting heatmap")
    public ResponseEntity<Map<String, BigDecimal>> getHeatmapData() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            return ResponseEntity.ok(bettingService.getHeatmapData(currentUser));
        } catch (Exception e) {
            logger.error("Błąd podczas pobierania danych heatmap: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }


    @PatchMapping("/{id}/settle")
    @Operation(summary = "Settle a bet", description = "Updates the status of a bet (e.g., WON, LOST, VOID) and calculates final profit")
    public ResponseEntity<?> settleBet(@PathVariable Long id, @Valid @RequestBody SettleBetRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Bet settledBet = bettingService.settleBet(id, request.status(), currentUser);
            return ResponseEntity.ok(convertToDto(settledBet));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error settling bet: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a bet", description = "Updates the details of an existing bet")
    public ResponseEntity<?> updateBet(@PathVariable Long id, @Valid @RequestBody BetRequest betRequest) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Bet updatedBet = bettingService.updateBet(id, betRequest, currentUser);
            return ResponseEntity.ok(convertToDto(updatedBet));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating bet: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a bet", description = "Removes a bet from the system")
    public ResponseEntity<?> deleteBet(@PathVariable Long id) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            bettingService.deleteBet(id, currentUser);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting bet: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
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
