package com.grzechuhehe.SportsBettingManagerApp.controller;

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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
public class BetController {
    private final BettingService bettingService;
    private final UserRepository userRepository;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BetController.class);

    @PostMapping("/add-bet")
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
            return ResponseEntity.status(500).body("Błąd podczas zapisywania zakładu: " + e.getMessage());
        }
    }

    @GetMapping
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
            return ResponseEntity.status(500).body(java.util.Collections.emptyList());
        }
    }


    @GetMapping("/dashboard-stats")
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
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            return ResponseEntity.ok(bettingService.getStatistics(currentUser));
        } catch (Exception e) {
            logger.error("Błąd podczas pobierania statystyk: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "totalStake", BigDecimal.ZERO,
                "profitLoss", BigDecimal.ZERO,
                "roi", BigDecimal.ZERO,
                "recentBets", java.util.Collections.emptyList()
            ));
        }
    }

    @GetMapping("/advanced-stats")
    public ResponseEntity<BetStatistics> getAdvancedStats() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            return ResponseEntity.ok(bettingService.getAdvancedStatistics(currentUser));
        } catch (Exception e) {
            logger.error("Błąd podczas pobierania zaawansowanych statystyk: {}", e.getMessage(), e);
            // Zwróć pusty obiekt statystyk
            BetStatistics emptyStats = new BetStatistics();
            return ResponseEntity.status(500).body(emptyStats);
        }
    }

    @GetMapping("/heatmap")
    public ResponseEntity<Map<String, BigDecimal>> getHeatmapData() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            return ResponseEntity.ok(bettingService.getHeatmapData(currentUser));
        } catch (Exception e) {
            logger.error("Błąd podczas pobierania danych heatmap: {}", e.getMessage(), e);
            // Zwróć pustą mapę
            return ResponseEntity.status(500).body(java.util.Collections.emptyMap());
        }
    }


    @PatchMapping("/{id}/settle")
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
            return ResponseEntity.status(500).body("Error settling bet: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
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
            return ResponseEntity.status(500).body("Error updating bet: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
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
            return ResponseEntity.status(500).body("Error deleting bet: " + e.getMessage());
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

