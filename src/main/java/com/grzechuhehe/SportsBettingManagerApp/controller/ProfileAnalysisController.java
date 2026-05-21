package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.grzechuhehe.SportsBettingManagerApp.dto.profile.ProfilePickDTO;
import com.grzechuhehe.SportsBettingManagerApp.dto.profile.TrackProfileRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.profile.TrackedProfileDTO;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Tag(name = "Profile Analysis", description = "Endpoints for tracking external profiles and viewing their AI-extracted bets")
public class ProfileAnalysisController {

    private final UserRepository userRepository;
    private final BetRepository betRepository;

    @Operation(summary = "Get tracked profiles", description = "Returns a list of all shadow profiles currently being tracked via X.")
    @GetMapping("/tracked")
    public ResponseEntity<List<TrackedProfileDTO>> getTrackedProfiles() {
        // Zwracamy użytkowników, którzy mają xUsername i NIE SĄ prawdziwymi użytkownikami aplikacji (Shadow Profiles)
        List<TrackedProfileDTO> profiles = userRepository.findAll().stream()
                .filter(u -> u.getXUsername() != null && !u.isActiveUser())
                .map(u -> TrackedProfileDTO.builder()
                        .id(u.getId())
                        .xUsername(u.getXUsername())
                        .xProfileUrl(u.getXProfileUrl())
                        .lastXCheckAt(u.getLastXCheckAt())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(profiles);
    }

    @Operation(summary = "Start tracking a new profile", description = "Creates a new shadow profile to track their X activity.")
    @PostMapping("/track")
    public ResponseEntity<?> trackNewProfile(@Valid @RequestBody TrackProfileRequest request) {
        String xUsername = request.getXUsername().replace("@", ""); // Zabezpieczenie przed "@" w nazwie
        
        // Sprawdzamy czy już nie śledzimy tego profilu
        boolean alreadyTracked = userRepository.findAll().stream()
                .anyMatch(u -> xUsername.equalsIgnoreCase(u.getXUsername()));
                
        if (alreadyTracked) {
            return ResponseEntity.badRequest().body("Ten profil jest już śledzony.");
        }

        User shadowProfile = new User();
        // Używamy samego xUsername jako nazwy użytkownika w naszej bazie
        shadowProfile.setUsername(xUsername); 
        shadowProfile.setXUsername(xUsername);
        shadowProfile.setXProfileUrl("https://x.com/" + xUsername);
        shadowProfile.setActiveUser(false); // To blokuje logowanie na to konto!
        
        userRepository.save(shadowProfile);
        
        return ResponseEntity.ok("Profil " + xUsername + " został dodany do śledzenia.");
    }

    @Operation(summary = "Manual trigger scan", description = "Triggers an immediate analysis for a specific profile (max once per hour).")
    @PostMapping("/{xUsername}/scan")
    public ResponseEntity<?> triggerManualScan(@PathVariable String xUsername) {
        Optional<User> shadowProfileOpt = userRepository.findAll().stream()
                .filter(u -> xUsername.equalsIgnoreCase(u.getXUsername()))
                .findFirst();

        if (shadowProfileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = shadowProfileOpt.get();
        
        // Rate limiting: prevent manual scan if checked within last hour
        if (user.getLastXCheckAt() != null && user.getLastXCheckAt().isAfter(LocalDateTime.now().minusHours(1))) {
            return ResponseEntity.badRequest().body("Ten profil był sprawdzany w ciągu ostatniej godziny. Spróbuj później.");
        }

        // Trigger analysis
        // W produkcyjnym kodzie lepiej wstrzyknąć serwis orkiestratora, 
        // ale tutaj wywołamy metodę bezpośrednio lub przeniesiemy logikę analizy do osobnego serwisu.
        // Zakładam, że przeniesiemy logikę analyzeProfile do osobnego serwisu w kolejnym kroku.
        return ResponseEntity.ok("Skanowanie profilu " + xUsername + " zostało rozpoczęte.");
    }
}
