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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    @Test
    void placeBet_ShouldCreateParlayBet_WhenMultipleBetsProvided() {
        // Given
        BetRequest leg1 = new BetRequest();
        leg1.setOdds(new BigDecimal("2.00"));
        leg1.setStake(new BigDecimal("50.00")); // Stake is taken from the first bet for Parlay logic
        
        BetRequest leg2 = new BetRequest();
        leg2.setOdds(new BigDecimal("1.50"));
        
        CreateBetRequest createBetRequest = new CreateBetRequest();
        createBetRequest.setBets(List.of(leg1, leg2));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(betRepository.save(any(Bet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<Bet> result = bettingService.placeBet(createBetRequest, "testuser");

        // Then
        assertThat(result).hasSize(1);
        Bet parlayBet = result.get(0);
        
        assertThat(parlayBet.getBetType()).isEqualTo(BetType.PARLAY);
        assertThat(parlayBet.getStatus()).isEqualTo(BetStatus.PENDING);
        assertThat(parlayBet.getStake()).isEqualByComparingTo("50.00");
        // Parlay odds: 2.00 * 1.50 = 3.00
        assertThat(parlayBet.getOdds()).isEqualByComparingTo("3.00");
        assertThat(parlayBet.getChildBets()).hasSize(2);
        
        // Verify child bets are linked correctly
        parlayBet.getChildBets().forEach(child -> {
            assertThat(child.getParentBet()).isEqualTo(parlayBet);
            assertThat(child.getStatus()).isEqualTo(BetStatus.PENDING);
            assertThat(child.getUser()).isEqualTo(testUser);
        });

        verify(betRepository, times(1)).save(any(Bet.class));
    }

    @Test
    void settleBet_ShouldUpdateStatusAndProfit_WhenWon() {
        // Given
        Bet bet = new Bet();
        bet.setId(10L);
        bet.setUser(testUser);
        bet.setStatus(BetStatus.PENDING);
        bet.setStake(new BigDecimal("100.00"));
        bet.setOdds(new BigDecimal("3.00"));
        bet.calculatePotentialWinnings(); // 300.00

        when(betRepository.findById(10L)).thenReturn(Optional.of(bet));
        when(betRepository.save(any(Bet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Bet settledBet = bettingService.settleBet(10L, BetStatus.WON, testUser);

        // Then
        assertThat(settledBet.getStatus()).isEqualTo(BetStatus.WON);
        assertThat(settledBet.getSettledAt()).isNotNull();
        // Profit = (100 * 3.00) - 100 = 200.00
        assertThat(settledBet.getFinalProfit()).isEqualByComparingTo("200.00");
        
        verify(betRepository).save(bet);
    }

        @Test

        void settleBet_ShouldUpdateStatusAndProfit_WhenLost() {

            // ... (poprzedni test)

            Bet bet = new Bet();

            bet.setId(11L);

            bet.setUser(testUser);

            bet.setStatus(BetStatus.PENDING);

            bet.setStake(new BigDecimal("50.00"));

            bet.setOdds(new BigDecimal("2.00"));

            bet.calculatePotentialWinnings();

    

            when(betRepository.findById(11L)).thenReturn(Optional.of(bet));

            when(betRepository.save(any(Bet.class))).thenAnswer(invocation -> invocation.getArgument(0));

    

            // When

            Bet settledBet = bettingService.settleBet(11L, BetStatus.LOST, testUser);

    

            // Then

            assertThat(settledBet.getStatus()).isEqualTo(BetStatus.LOST);

            // Profit = -50.00

            assertThat(settledBet.getFinalProfit()).isEqualByComparingTo("-50.00");

        }

    

        @Test

        void updateBet_ShouldUpdateAllFieldsAndRecalculateWinnings() {

            // Given

            Bet bet = new Bet();

            bet.setId(20L);

            bet.setUser(testUser);

            bet.setStake(new BigDecimal("10.00"));

            bet.setOdds(new BigDecimal("2.00"));

            bet.calculatePotentialWinnings(); // 20.00

    

            BetRequest updateRequest = new BetRequest();

            updateRequest.setEventName("New Event");

            updateRequest.setStake(new BigDecimal("20.00"));

            updateRequest.setOdds(new BigDecimal("3.00"));

            updateRequest.setSport("Tennis");

    

            when(betRepository.findById(20L)).thenReturn(Optional.of(bet));

            when(betRepository.save(any(Bet.class))).thenAnswer(invocation -> invocation.getArgument(0));

    

            // When

            Bet result = bettingService.updateBet(20L, updateRequest, testUser);

    

            // Then

            assertThat(result.getEventName()).isEqualTo("New Event");

            assertThat(result.getStake()).isEqualByComparingTo("20.00");

            assertThat(result.getOdds()).isEqualByComparingTo("3.00");

            assertThat(result.getPotentialWinnings()).isEqualByComparingTo("60.00"); // 20 * 3

            verify(betRepository).save(bet);

        }

    

        @Test

        void deleteBet_ShouldInvokeRepositoryDelete() {

            // Given

            Bet bet = new Bet();

            bet.setId(30L);

            bet.setUser(testUser);

    

            when(betRepository.findById(30L)).thenReturn(Optional.of(bet));

    

            // When

            bettingService.deleteBet(30L, testUser);

    

            // Then

            verify(betRepository, times(1)).delete(bet);

        }

    

        @Test

        void settleBet_ShouldThrowException_WhenUserIsNotOwner() {

            // Given

            User otherUser = new User();

            otherUser.setId(99L);

            

            Bet bet = new Bet();

            bet.setId(40L);

            bet.setUser(otherUser);

    

            when(betRepository.findById(40L)).thenReturn(Optional.of(bet));

    

            // When & Then

            assertThrows(IllegalArgumentException.class, () -> 

                bettingService.settleBet(40L, BetStatus.WON, testUser)

            );

        }

    }

    