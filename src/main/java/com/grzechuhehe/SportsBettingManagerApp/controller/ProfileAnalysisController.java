package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.grzechuhehe.SportsBettingManagerApp.dto.DashboardStatsDTO;
import com.grzechuhehe.SportsBettingManagerApp.dto.BetStatistics;
import com.grzechuhehe.SportsBettingManagerApp.dto.PageResponse;
import com.grzechuhehe.SportsBettingManagerApp.dto.profile.ProfilePickDTO;
import com.grzechuhehe.SportsBettingManagerApp.dto.profile.TrackProfileRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.profile.TrackedProfileDTO;
import com.grzechuhehe.SportsBettingManagerApp.integration.SocialDataClient;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.BettingService;
import com.grzechuhehe.SportsBettingManagerApp.service.ProfileAnalysisOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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
    private final SocialDataClient socialDataClient;
    private final BettingService bettingService;
    private final ProfileAnalysisOrchestrator profileAnalysisOrchestrator;

    @Operation(summary = "Get tracked profiles", description = "Returns a list of all profiles currently being tracked via X.")
    @GetMapping("/tracked")
    public ResponseEntity<List<TrackedProfileDTO>> getTrackedProfiles() {
        // Zwracamy shadow profile (isActiveUser=false) LUB użytkowników z przypiętym kontem X (xUsername!=null)
        List<TrackedProfileDTO> profiles = userRepository.findAll().stream()
                .filter(u -> !u.isActiveUser() || u.getXUsername() != null)
                .map(u -> TrackedProfileDTO.builder()
                        .id(u.getId())
                        // FALLBACK: jeśli xUsername w starej bazie jest null, używamy username
                        .xUsername(u.getXUsername() != null ? u.getXUsername() : u.getUsername())
                        .xProfileUrl(u.getXProfileUrl())
                        .lastXCheckAt(u.getLastXCheckAt())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(profiles);
    }

    @Operation(summary = "Start tracking a new profile", description = "Creates a new shadow profile to track their X activity. Verifies existence on X.")
    @PostMapping("/track")
    public ResponseEntity<?> trackNewProfile(@Valid @RequestBody TrackProfileRequest request) {
        String xUsername = request.getXUsername().replace("@", "").trim(); 
        
        // Sprawdzamy czy już nie śledzimy tego profilu (szukamy po xUsername ORAZ po username)
        boolean alreadyTracked = userRepository.findAll().stream()
                .anyMatch(u -> xUsername.equalsIgnoreCase(u.getXUsername()) || xUsername.equalsIgnoreCase(u.getUsername()));
                
        if (alreadyTracked) {
            return ResponseEntity.badRequest().body("This profile is already being tracked.");
        }

        // Weryfikacja czy profil istnieje na platformie X
        boolean profileExistsOnX = socialDataClient.checkProfileExists(xUsername);
        if (!profileExistsOnX) {
            return ResponseEntity.badRequest().body("Profile @" + xUsername + " does not exist on X or is suspended.");
        }

        User shadowProfile = new User();
        shadowProfile.setUsername(xUsername); 
        shadowProfile.setXUsername(xUsername);
        shadowProfile.setXProfileUrl("https://x.com/" + xUsername);
        shadowProfile.setActiveUser(false); 
        
        User savedProfile = userRepository.save(shadowProfile);
        
        // Trigger immediate analysis for new profile
        try {
            profileAnalysisOrchestrator.processSingleProfile(savedProfile);
        } catch (Exception e) {
            // Log error but don't fail the track request as the profile is already saved
            // Orchestrator already logs errors, but we might want a simple message here
        }
        
        return ResponseEntity.ok("Profile " + xUsername + " added to tracking and initial scan triggered.");
    }

    @Operation(summary = "Manual trigger scan", description = "Triggers an immediate analysis for a specific profile (max once per hour).")
    @PostMapping("/{xUsername}/scan")
    public ResponseEntity<?> triggerManualScan(@PathVariable String xUsername) {
        Optional<User> shadowProfileOpt = userRepository.findByXUsernameIgnoreCase(xUsername);

        if (shadowProfileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = shadowProfileOpt.get();
        
        // Rate limiting: prevent manual scan if checked within last hour
        if (user.getLastXCheckAt() != null && user.getLastXCheckAt().isAfter(LocalDateTime.now().minusHours(1))) {
            return ResponseEntity.badRequest().body("This profile was checked within the last hour. Please try again later.");
        }

        // Trigger analysis
        try {
            profileAnalysisOrchestrator.processSingleProfile(user);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error during manual scan: " + e.getMessage());
        }
        
        return ResponseEntity.ok("Scan for profile " + xUsername + " has been successfully completed.");
    }

    @Operation(summary = "Get picks for a profile", description = "Returns AI-extracted bets for the specified X profile.")
    @GetMapping("/{xUsername}/picks")
    public ResponseEntity<PageResponse<ProfilePickDTO>> getProfilePicks(
            @PathVariable String xUsername,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
            
        Optional<User> shadowProfileOpt = userRepository.findByXUsernameIgnoreCase(xUsername);

        if (shadowProfileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = shadowProfileOpt.get();
        
        org.springframework.data.domain.Page<Bet> betPage = betRepository.findRootAiBetsByUser(
                user, 
                org.springframework.data.domain.PageRequest.of(page, size, Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "placedAt"))
        );
        
        org.springframework.data.domain.Page<ProfilePickDTO> picks = betPage.map(this::mapToProfilePickDTO);

        return ResponseEntity.ok(PageResponse.fromPage(picks));
    }

    @Operation(summary = "Get statistics for a profile", description = "Returns dashboard-like statistics for the specified X profile.")
    @GetMapping("/{xUsername}/stats")
    public ResponseEntity<DashboardStatsDTO> getProfileStats(@PathVariable String xUsername) {
        Optional<User> shadowProfileOpt = userRepository.findByXUsernameIgnoreCase(xUsername);

        if (shadowProfileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = shadowProfileOpt.get();
        DashboardStatsDTO stats = bettingService.getDashboardStats(user);
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Get advanced statistics for a profile", description = "Returns advanced statistics like Sharpe ratio and Efficiency.")
    @GetMapping("/{xUsername}/advanced-stats")
    public ResponseEntity<BetStatistics> getProfileAdvancedStats(@PathVariable String xUsername) {
        Optional<User> shadowProfileOpt = userRepository.findByXUsernameIgnoreCase(xUsername);

        if (shadowProfileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = shadowProfileOpt.get();
        BetStatistics stats = bettingService.getAdvancedStatistics(user);
        return ResponseEntity.ok(stats);
    }

    private ProfilePickDTO mapToProfilePickDTO(Bet b) {
        return ProfilePickDTO.builder()
                .id(b.getId())
                .eventName(b.getEventName())
                .selection(b.getSelection())
                .odds(b.getOdds())
                .units(b.getUnits())
                .stake(b.getStake())
                .bookmaker(b.getBookmaker())
                .status(b.getStatus())
                .imageProofPath(b.getImageProofPath())
                .placedAt(b.getPlacedAt())
                .sourcePostId(b.getSourcePostId())
                .isPreMatch(b.isPreMatch())
                .legs(b.getChildBets() != null ? b.getChildBets().stream().map(this::mapToProfilePickDTO).collect(Collectors.toList()) : null)
                .build();
    }

    @Operation(summary = "Search for a tracked profile", description = "Returns profile details if tracked, 404 otherwise.")
    @GetMapping("/search")
    public ResponseEntity<TrackedProfileDTO> searchProfile(@RequestParam String query) {
        String xUsername = query.replace("@", "").trim();
        Optional<User> userOpt = userRepository.findByXUsernameIgnoreCase(xUsername);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User u = userOpt.get();
        return ResponseEntity.ok(TrackedProfileDTO.builder()
                .id(u.getId())
                .xUsername(u.getXUsername())
                .xProfileUrl(u.getXProfileUrl())
                .lastXCheckAt(u.getLastXCheckAt())
                .build());
    }
}
