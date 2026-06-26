package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.ImageStorageService;
import com.grzechuhehe.SportsBettingManagerApp.service.ProfileAnalysisOrchestrator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BetImportControllerTest {

    @Mock private UserRepository userRepository;
    @Mock private ImageStorageService imageStorageService;
    @Mock private ProfileAnalysisOrchestrator orchestrator;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BetImportController controller =
                new BetImportController(userRepository, imageStorageService, orchestrator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsOkWithExtractedBet() throws Exception {
        User user = new User();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(imageStorageService.saveUploadedImage(any(), eq("manual/john")))
                .thenReturn("/images/profiles/manual/john/abc.png");
        Bet bet = Bet.builder()
                .id(99L).eventName("Real Madrid - Barcelona").selection("Over 2.5")
                .odds(new BigDecimal("1.85")).betType(BetType.SINGLE).status(BetStatus.PENDING)
                .imageProofPath("/images/profiles/manual/john/abc.png")
                .build();
        when(orchestrator.importBetFromImages(eq(user), anyList(), any()))
                .thenReturn(Optional.of(bet));

        MockMultipartFile file = new MockMultipartFile(
                "image", "kupon.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/bets/import-from-image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.eventName").value("Real Madrid - Barcelona"));
    }

    @Test
    void returns422WhenExtractionFails() throws Exception {
        User user = new User();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(imageStorageService.saveUploadedImage(any(), eq("manual/john")))
                .thenReturn("/images/profiles/manual/john/abc.png");
        when(orchestrator.importBetFromImages(eq(user), anyList(), any()))
                .thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "image", "kupon.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/bets/import-from-image").file(file))
                .andExpect(status().isUnprocessableEntity());
    }
}
