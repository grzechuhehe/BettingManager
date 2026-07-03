package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.dto.profile.ProfilePreviewDTO;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfilePreviewServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BettingService bettingService;

    @InjectMocks
    private ProfilePreviewService service;

    @Test
    void buildPreview_trackedProfileWithStats() {
        LocalDateTime trackedSince = LocalDateTime.of(2026, 3, 15, 10, 0);
        User user = new User();
        user.setId(1L);
        user.setXUsername("guru");
        user.setXProfileUrl("https://x.com/guru");
        user.setJoinedAt(trackedSince);
        when(userRepository.findByXUsernameIgnoreCase("guru")).thenReturn(Optional.of(user));
        when(bettingService.getProfileSummaryStats(user)).thenReturn(
                new BettingService.ProfileSummaryStats(10, new BigDecimal("60"), new BigDecimal("5")));

        ProfilePreviewDTO preview = service.buildPreview("guru");

        assertThat(preview.isTracked()).isTrue();
        assertThat(preview.getTotalBets()).isEqualTo(10);
        assertThat(preview.getTrackedSince()).isEqualTo(trackedSince);
    }

    @Test
    void buildPreview_untrackedProfile() {
        when(userRepository.findByXUsernameIgnoreCase("newguy")).thenReturn(Optional.empty());

        ProfilePreviewDTO preview = service.buildPreview("newguy");

        assertThat(preview.isTracked()).isFalse();
        assertThat(preview.getXUsername()).isEqualTo("newguy");
        assertThat(preview.getTotalBets()).isNull();
    }
}
