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
@RequestMapping("/bets")
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(bettingService.getStatistics(user));
    }

    @GetMapping("/advanced-stats/{userId}")
    public ResponseEntity<BetStatistics> getAdvancedStats(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(bettingService.getAdvancedStatistics(user));
    }

    @GetMapping("/heatmap/{userId}")
    public ResponseEntity<Map<String, Map<String, Double>>> getHeatmapData(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(bettingService.getHeatmapData(user));
    }
}