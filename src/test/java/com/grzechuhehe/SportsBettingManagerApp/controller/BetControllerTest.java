package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.config.AuthEntryPointJwt;
import com.grzechuhehe.SportsBettingManagerApp.dto.BetRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.CreateBetRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.DashboardStatsDTO;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.BettingService;
import com.grzechuhehe.SportsBettingManagerApp.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BetController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simplicity, mocking context instead
class BetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BettingService bettingService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private AuthEntryPointJwt authEntryPointJwt;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        // Mock security context behavior manually since filters are disabled but controller calls context
        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createBet_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        // Given
        BetRequest betRequest = new BetRequest();
        betRequest.setStake(new BigDecimal("100.00"));
        betRequest.setOdds(new BigDecimal("2.50"));
        betRequest.setSport("Football");
        betRequest.setEventName("Event A");
        betRequest.setSelection("Team A");
        // Required fields added to pass validation
        betRequest.setEventDate(LocalDateTime.now().plusDays(1));
        betRequest.setMarketType(MarketType.MONEYLINE_1X2);
        betRequest.setBookmaker("TestBookmaker");
        CreateBetRequest createRequest = new CreateBetRequest();
        createRequest.setBets(List.of(betRequest));
        Bet savedBet = new Bet();
        savedBet.setId(1L);
        savedBet.setBetType(BetType.SINGLE);
        savedBet.setStake(new BigDecimal("100.00"));
        savedBet.setOdds(new BigDecimal("2.50"));
        savedBet.setStatus(BetStatus.PENDING);
        savedBet.setUser(testUser);

        Mockito.when(bettingService.placeBet(any(CreateBetRequest.class), eq("testuser")))
                .thenReturn(List.of(savedBet));

        // When & Then
        mockMvc.perform(post("/api/bets/add-bet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].stake").value(100.00))
                .andExpect(jsonPath("$[0].odds").value(2.50));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getDashboardStats_ShouldReturnStats() throws Exception {
        // Given
        DashboardStatsDTO stats = new DashboardStatsDTO(
                new BigDecimal("150.50"), // totalProfitLoss
                100,                      // totalBets
                new BigDecimal("65.5"),   // winRate
                5                         // activeBetsCount
        );

        Mockito.when(bettingService.getDashboardStats(any(User.class))).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/bets/dashboard-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProfitLoss").value(150.50))
                .andExpect(jsonPath("$.winRate").value(65.5))
                .andExpect(jsonPath("$.activeBetsCount").value(5));
    }

    @Test
    @WithMockUser(username = "testuser")
    void deleteBet_ShouldReturnOk_WhenBetExists() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/bets/{id}", 10L))
                .andExpect(status().isOk());

        Mockito.verify(bettingService).deleteBet(eq(10L), any(User.class));
    }
}
