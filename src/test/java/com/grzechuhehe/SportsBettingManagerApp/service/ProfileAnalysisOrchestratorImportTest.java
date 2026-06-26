package com.grzechuhehe.SportsBettingManagerApp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.integration.GeminiVisionClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.SocialDataClient;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileAnalysisOrchestratorImportTest {

    @Mock private UserRepository userRepository;
    @Mock private BetRepository betRepository;
    @Mock private SocialDataClient socialDataClient;
    @Mock private GeminiVisionClient geminiVisionClient;
    @Mock private ImageStorageService imageStorageService;

    private ProfileAnalysisOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new ProfileAnalysisOrchestrator(
                userRepository, betRepository, socialDataClient,
                geminiVisionClient, imageStorageService, new ObjectMapper());
    }

    @Test
    void importBuildsAndSavesBetFromGeminiJson() {
        String json = "{\"isPlacedBet\":true,\"eventName\":\"Real Madrid - Barcelona\","
                + "\"selection\":\"Over 2.5\",\"odds\":1.85,\"stake\":50,"
                + "\"sport\":\"Football\",\"marketType\":\"TOTALS_OVER_UNDER\",\"status\":\"PENDING\"}";
        when(geminiVisionClient.analyzeBet(any(), anyList())).thenReturn(json);
        when(betRepository.save(any(Bet.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = new User();
        Optional<Bet> result = orchestrator.importBetFromImages(
                user, List.of("/images/profiles/manual/john/abc.png"), null);

        assertTrue(result.isPresent());
        Bet bet = result.get();
        assertEquals("Real Madrid - Barcelona", bet.getEventName());
        assertEquals(0, new java.math.BigDecimal("1.85").compareTo(bet.getOdds()));
        assertTrue(bet.isAiExtracted());
        assertEquals("/images/profiles/manual/john/abc.png", bet.getImageProofPath());
        assertEquals(BetStatus.PENDING, bet.getStatus());
        assertSame(user, bet.getUser());
    }

    @Test
    void importReturnsEmptyWhenExtractionInvalid() {
        String json = "{\"isPlacedBet\":true,\"selection\":\"???\"}";
        when(geminiVisionClient.analyzeBet(any(), anyList())).thenReturn(json);

        Optional<Bet> result = orchestrator.importBetFromImages(
                new User(), List.of("/images/profiles/manual/john/abc.png"), null);

        assertTrue(result.isEmpty());
    }
}
