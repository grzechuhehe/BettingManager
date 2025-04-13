package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.dto.BetStatistics;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.BettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
public class BetController {
    private final BettingService bettingService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createBet(@Valid @RequestBody Bet bet, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getFieldErrors());
        }
        return ResponseEntity.ok(bettingService.placeBet(bet));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(@RequestParam Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(bettingService.getStatistics(user));
        } catch (Exception e) {
            // Obsługa błędu, żeby zapobiec rzucaniu nieprzechwyconych wyjątków
            // Możemy zwrócić częściowe dane lub dane puste
            return ResponseEntity.ok(Map.of(
                "totalAmount", 0.0,
                "profitLoss", 0.0,
                "roi", 0.0,
                "recentBets", java.util.Collections.emptyList()
            ));
        }
    }

    @GetMapping("/advanced-stats/{userId}")
    public ResponseEntity<BetStatistics> getAdvancedStats(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(bettingService.getAdvancedStatistics(user));
        } catch (Exception e) {
            // Zwróć pusty obiekt statystyk
            BetStatistics emptyStats = new BetStatistics();
            return ResponseEntity.ok(emptyStats);
        }
    }

    @GetMapping("/heatmap/{userId}")
    public ResponseEntity<Map<String, Map<String, Double>>> getHeatmapData(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(bettingService.getHeatmapData(user));
        } catch (Exception e) {
            // Zwróć pustą mapę
            return ResponseEntity.ok(java.util.Collections.emptyMap());
        }
    }
}

