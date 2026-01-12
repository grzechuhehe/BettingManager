package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.dto.BetRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.CreateBetRequest;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BettingServiceTest {

    @Mock
    private BetRepository betRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BettingService bettingService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    void placeBet_ShouldCreateSingleBet_WhenRequestIsValid() {
        // Given
        BetRequest betRequest = new BetRequest();
        betRequest.setStake(new BigDecimal("100.00"));
        betRequest.setOdds(new BigDecimal("2.50"));
        betRequest.setSport("Football");
        betRequest.setEventName("Team A vs Team B");
        betRequest.setSelection("Team A");

        CreateBetRequest createBetRequest = new CreateBetRequest();
        createBetRequest.setBets(List.of(betRequest));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(betRepository.save(any(Bet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<Bet> result = bettingService.placeBet(createBetRequest, "testuser");

        // Then
        assertThat(result).hasSize(1);
        Bet savedBet = result.get(0);
        assertThat(savedBet.getBetType()).isEqualTo(BetType.SINGLE);
        assertThat(savedBet.getStatus()).isEqualTo(BetStatus.PENDING);
        assertThat(savedBet.getStake()).isEqualByComparingTo("100.00");
        assertThat(savedBet.getOdds()).isEqualByComparingTo("2.50");
        assertThat(savedBet.getPotentialWinnings()).isEqualByComparingTo("250.00"); // 100 * 2.5
        assertThat(savedBet.getUser()).isEqualTo(testUser);

        verify(betRepository, times(1)).save(any(Bet.class));
    }

    @Test
    void placeBet_ShouldThrowException_WhenUserNotFound() {
        // Given
        CreateBetRequest request = new CreateBetRequest();
        request.setBets(List.of(new BetRequest()));
        
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            bettingService.placeBet(request, "unknown")
        );
        verify(betRepository, never()).save(any());
    }
}
