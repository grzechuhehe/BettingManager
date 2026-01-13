package com.grzechuhehe.SportsBettingManagerApp.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserProfileDTO(
    String username,
    String email,
    LocalDateTime joinedAt,
    List<String> roles
) {}
