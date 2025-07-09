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
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BetController.class);
        
        logger.info("Otrzymano żądanie utworzenia zakładu: {}", bet);
        
        // Sprawdź, czy użytkownik jest ustawiony
        if (bet.getUser() == null || bet.getUser().getId() == null) {
            logger.error("Brak informacji o użytkowniku w żądaniu");
            return ResponseEntity.badRequest().body("Brak informacji o użytkowniku");
        }
        
        // Sprawdź, czy wydarzenie jest ustawione
        if (bet.getEvent() == null) {
            logger.error("Brak informacji o wydarzeniu w żądaniu");
            return ResponseEntity.badRequest().body("Brak informacji o wydarzeniu");
        }
        
        // Sprawdź błędy walidacji
        if (result.hasErrors()) {
            logger.error("Błędy walidacji: {}", result.getFieldErrors());
            return ResponseEntity.badRequest().body(result.getFieldErrors());
        }
        
        try {
            logger.info("Zapisuję zakład dla użytkownika o ID: {}", bet.getUser().getId());
            Bet savedBet = bettingService.placeBet(bet);
            logger.info("Zakład zapisany pomyślnie, ID: {}", savedBet.getId());
            return ResponseEntity.ok(savedBet);
        } catch (Exception e) {
            logger.error("Błąd podczas zapisywania zakładu: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Błąd podczas zapisywania zakładu: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<java.util.List<Bet>> getAllBetsForCurrentUser() {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BetController.class);
        try {
            // Pobierz aktualnie uwierzytelnionego użytkownika
            User currentUser = (User) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            java.util.List<Bet> bets = bettingService.getUserBets(currentUser);
            logger.info("Pobrano {} zakładów dla użytkownika: {}", bets.size(), currentUser.getUsername());
            return ResponseEntity.ok(bets);
        } catch (Exception e) {
            logger.error("Błąd podczas pobierania zakładów dla bieżącego użytkownika: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(java.util.Collections.emptyList());
        }
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

