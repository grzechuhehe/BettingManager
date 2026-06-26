package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.grzechuhehe.SportsBettingManagerApp.dto.ImportedBetResponse;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.ImageStorageService;
import com.grzechuhehe.SportsBettingManagerApp.service.ProfileAnalysisOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
@Tag(name = "Bet Import", description = "Import a bet by uploading a screenshot/photo analyzed by Gemini Vision")
public class BetImportController {

    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final ProfileAnalysisOrchestrator orchestrator;

    @PostMapping(value = "/import-from-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import bet from image",
            description = "Uploads a bet screenshot/photo, extracts bet data via Gemini Vision and saves it for the current user.")
    public ResponseEntity<?> importFromImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "note", required = false) String note) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String imagePath;
        try {
            imagePath = imageStorageService.saveUploadedImage(image, "manual/" + username);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "invalid_file",
                    "message", e.getMessage()));
        }

        Optional<Bet> bet = orchestrator.importBetFromImages(user, List.of(imagePath), note);
        if (bet.isEmpty()) {
            return ResponseEntity.unprocessableEntity().body(Map.of(
                    "status", "rejected",
                    "message", "Nie udało się odczytać poprawnego zakładu ze zdjęcia.",
                    "imageProofPath", imagePath));
        }
        return ResponseEntity.ok(ImportedBetResponse.from(bet.get()));
    }
}
