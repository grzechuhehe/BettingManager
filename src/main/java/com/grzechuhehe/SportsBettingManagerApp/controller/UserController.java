package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.grzechuhehe.SportsBettingManagerApp.dto.ChangePasswordRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.UserProfileDTO;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Operations related to user account and settings")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Get User Profile", description = "Returns the profile information of the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getProfile() {
        User user = getCurrentUser();
        return ResponseEntity.ok(new UserProfileDTO(
                user.getUsername(),
                user.getEmail(),
                user.getJoinedAt(),
                user.getRoles()
        ));
    }

    @Operation(summary = "Change Password", description = "Updates the password for the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "Password changed successfully")
    @ApiResponse(responseCode = "400", description = "Incorrect old password or invalid new password")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Incorrect old password"));
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
