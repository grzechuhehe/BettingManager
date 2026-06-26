package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.integration.SocialDataClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.GeminiVisionClient;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProfileAnalysisOrchestratorTest {

    @Mock
    private SocialDataClient socialDataClient;
    @Mock
    private GeminiVisionClient geminiVisionClient;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BetRepository betRepository;

    @InjectMocks
    private ProfileAnalysisOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void processSingleProfile_ShouldFetch100Tweets_WhenFirstTimeScanned() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setXUsername("new_user");
        user.setLastScrapedTweetId(null); // First time

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // Przekazujemy limit 100
        when(socialDataClient.fetchRecentTweets(eq("new_user"), isNull(), eq(100))).thenReturn(Collections.emptyList());

        // When
        orchestrator.processSingleProfile(user);

        // Then
        verify(socialDataClient).fetchRecentTweets(eq("new_user"), isNull(), eq(100));
    }

    @Test
    void processSingleProfile_ShouldFetchDefault40Tweets_WhenAlreadyScanned() {
        // Given
        User user = new User();
        user.setId(2L);
        user.setXUsername("old_user");
        user.setLastScrapedTweetId("12345");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        // Skan przyrostowy pobiera 40 najnowszych postów (od ostatniego znanego ID)
        when(socialDataClient.fetchRecentTweets(eq("old_user"), eq("12345"), eq(40))).thenReturn(Collections.emptyList());

        // When
        orchestrator.processSingleProfile(user);

        // Then
        verify(socialDataClient).fetchRecentTweets(eq("old_user"), eq("12345"), eq(40));
    }
}
