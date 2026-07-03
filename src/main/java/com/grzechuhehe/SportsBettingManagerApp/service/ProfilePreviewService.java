package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.dto.profile.ProfilePreviewDTO;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfilePreviewService {

    private final UserRepository userRepository;
    private final BettingService bettingService;

    public ProfilePreviewDTO buildPreview(String rawQuery) {
        String xUsername = rawQuery.replace("@", "").trim();
        return userRepository.findByXUsernameIgnoreCase(xUsername)
                .map(this::buildTrackedPreview)
                .orElseGet(() -> ProfilePreviewDTO.builder()
                        .xUsername(xUsername)
                        .xProfileUrl("https://x.com/" + xUsername)
                        .tracked(false)
                        .build());
    }

    private ProfilePreviewDTO buildTrackedPreview(User user) {
        BettingService.ProfileSummaryStats stats = bettingService.getProfileSummaryStats(user);

        return ProfilePreviewDTO.builder()
                .xUsername(user.getXUsername())
                .xProfileUrl(user.getXProfileUrl())
                .tracked(true)
                .trackedSince(user.getJoinedAt())
                .lastXCheckAt(user.getLastXCheckAt())
                .totalBets(stats.totalBets())
                .winRate(stats.winRate())
                .yield(stats.yield())
                .build();
    }
}
