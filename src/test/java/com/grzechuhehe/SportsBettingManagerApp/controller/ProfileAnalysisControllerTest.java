package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.dto.profile.TrackProfileRequest;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProfileAnalysisControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BetRepository betRepository;

    @InjectMocks
    private ProfileAnalysisController profileAnalysisController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(profileAnalysisController).build();
    }

    @Test
    void trackNewProfile_ShouldCreateShadowProfile_WhenValidUsernameProvided() throws Exception {
        // Given
        TrackProfileRequest request = new TrackProfileRequest();
        request.setXUsername("nowyguru");

        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(userRepository.save(any(User.class))).thenReturn(new User());

        // When & Then
        mockMvc.perform(post("/api/profiles/track")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Profile nowyguru added to tracking."));

        verify(userRepository, times(1)).save(argThat(user -> 
            !user.isActiveUser() && 
            "nowyguru".equals(user.getUsername()) &&
            "nowyguru".equals(user.getXUsername()) &&
            "https://x.com/nowyguru".equals(user.getXProfileUrl())
        ));
    }

    @Test
    void trackNewProfile_ShouldStripAtSymbolAndSave() throws Exception {
        TrackProfileRequest request = new TrackProfileRequest();
        request.setXUsername("@hacker"); // Edge case: użytkownik wpisuje z małpą

        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/api/profiles/track")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userRepository).save(argThat(user -> "hacker".equals(user.getXUsername())));
    }

    @Test
    void triggerManualScan_ShouldReturnBadRequest_IfScannedWithinLastHour() throws Exception {
        User shadowProfile = new User();
        shadowProfile.setXUsername("spamguru");
        // Ustawiamy czas skanowania na 10 minut temu (za wcześnie na kolejny skan)
        shadowProfile.setLastXCheckAt(LocalDateTime.now().minusMinutes(10));

        when(userRepository.findAll()).thenReturn(Collections.singletonList(shadowProfile));

        mockMvc.perform(post("/api/profiles/spamguru/scan")
                .characterEncoding("UTF-8"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("This profile was checked within the last hour. Please try again later."));
    }

    @Test
    void triggerManualScan_ShouldReturnOk_IfNeverScanned() throws Exception {
        User shadowProfile = new User();
        shadowProfile.setXUsername("freshguru");
        shadowProfile.setLastXCheckAt(null); // Nigdy nie skanowany

        when(userRepository.findAll()).thenReturn(Collections.singletonList(shadowProfile));

        mockMvc.perform(post("/api/profiles/freshguru/scan")
                .characterEncoding("UTF-8"))
                .andExpect(status().isOk());
    }
}
