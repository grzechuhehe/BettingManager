package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.dto.BetRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.BetStatistics;
import com.grzechuhehe.SportsBettingManagerApp.dto.CreateBetRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.DashboardStatsDTO;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import com.grzechuhehe.SportsBettingManagerApp.dto.BetStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        assertThat(savedBet.getPotentialWinnings()).isEqualByComparingTo("250.00");
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
        leg1.setStake(new BigDecimal("50.00"));
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
        assertThat(parlayBet.getOdds()).isEqualByComparingTo("3.00");
        assertThat(parlayBet.getChildBets()).hasSize(2);
        
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
        bet.calculatePotentialWinnings();

        when(betRepository.findById(10L)).thenReturn(Optional.of(bet));
        when(betRepository.save(any(Bet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Bet settledBet = bettingService.settleBet(10L, BetStatus.WON, testUser);

        // Then
        assertThat(settledBet.getStatus()).isEqualTo(BetStatus.WON);
        assertThat(settledBet.getSettledAt()).isNotNull();
        assertThat(settledBet.getFinalProfit()).isEqualByComparingTo("200.00");
        verify(betRepository).save(bet);
    }

    @Test
    void updateBet_ShouldUpdateAllFieldsAndRecalculateWinnings() {
        // Given
        Bet bet = new Bet();
        bet.setId(20L);
        bet.setUser(testUser);
        bet.setStake(new BigDecimal("10.00"));
        bet.setOdds(new BigDecimal("2.00"));
        bet.calculatePotentialWinnings();
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
        assertThat(result.getPotentialWinnings()).isEqualByComparingTo("60.00");
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
    void getHeatmapData_ShouldGroupProfitsByDate() {
        // Given
        Bet bet1 = new Bet();
        bet1.setSettledAt(LocalDate.of(2023, 1, 1).atStartOfDay());
        bet1.setFinalProfit(new BigDecimal("100.00"));
        bet1.setStatus(BetStatus.WON);
        
        Bet bet2 = new Bet();
        bet2.setSettledAt(LocalDate.of(2023, 1, 1).atStartOfDay());
        bet2.setFinalProfit(new BigDecimal("50.00"));
        bet2.setStatus(BetStatus.WON);

        Bet bet3 = new Bet();
        bet3.setSettledAt(LocalDate.of(2023, 1, 2).atStartOfDay());
        bet3.setFinalProfit(new BigDecimal("-20.00"));
        bet3.setStatus(BetStatus.LOST);

        // Używamy findByUser zamiast findByUserAndSettledAtIsNotNull
        when(betRepository.findByUser(testUser)).thenReturn(Arrays.asList(bet1, bet2, bet3));

        // When
        Map<String, BigDecimal> heatmap = bettingService.getHeatmapData(testUser);

        // Then
        assertThat(heatmap).hasSize(2);
        assertThat(heatmap.get("2023-01-01")).isEqualByComparingTo("150.00");
        assertThat(heatmap.get("2023-01-02")).isEqualByComparingTo("-20.00");
    }

    @Test
    void getDashboardStats_ShouldCalculateBasicMetrics() {
        // Given
        Bet win1 = new Bet();
        win1.setStatus(BetStatus.WON);
        win1.setStake(new BigDecimal("100"));
        win1.setFinalProfit(new BigDecimal("100"));
        win1.setSport("Football");

        Bet win2 = new Bet();
        win2.setStatus(BetStatus.WON);
        win2.setStake(new BigDecimal("100"));
        win2.setFinalProfit(new BigDecimal("50"));
        win2.setSport("Tennis");

        Bet loss = new Bet();
        loss.setStatus(BetStatus.LOST);
        loss.setStake(new BigDecimal("100"));
        loss.setFinalProfit(new BigDecimal("-100"));
        loss.setSport("Football");

        when(betRepository.findByUser(testUser)).thenReturn(Arrays.asList(win1, win2, loss));

        // When
        DashboardStatsDTO stats = bettingService.getDashboardStats(testUser);

        // Then
        assertThat(stats.totalBets()).isEqualTo(3);
        assertThat(stats.winRate()).isEqualByComparingTo("66.67");
        assertThat(stats.totalProfitLoss()).isEqualByComparingTo("50.00");
        assertThat(stats.totalStaked()).isEqualByComparingTo("300.00");
        assertThat(stats.roi()).isEqualByComparingTo("16.67"); // 50 / 300 * 100
        assertThat(stats.profitBySport().get("Football")).isEqualByComparingTo("0.00"); // 100 - 100
        assertThat(stats.profitBySport().get("Tennis")).isEqualByComparingTo("50.00");
    }

    @Test
    void getDashboardStats_ShouldCalculateAdvancedAnalytics() {
        // Given
        Bet bet1 = new Bet();
        bet1.setStatus(BetStatus.WON);
        bet1.setStake(new BigDecimal("100"));
        bet1.setFinalProfit(new BigDecimal("50"));
        bet1.setSettledAt(LocalDate.of(2023, 1, 1).atStartOfDay());

        Bet bet2 = new Bet();
        bet2.setStatus(BetStatus.WON);
        bet2.setStake(new BigDecimal("100"));
        bet2.setFinalProfit(new BigDecimal("30"));
        bet2.setSettledAt(LocalDate.of(2023, 1, 1).atTime(12, 0)); // Ta sama data, inna godzina

        Bet bet3 = new Bet();
        bet3.setStatus(BetStatus.LOST);
        bet3.setStake(new BigDecimal("100"));
        bet3.setFinalProfit(new BigDecimal("-40"));
        bet3.setSettledAt(LocalDate.of(2023, 1, 2).atStartOfDay());

        when(betRepository.findByUser(testUser)).thenReturn(Arrays.asList(bet1, bet2, bet3));

        // When
        DashboardStatsDTO stats = bettingService.getDashboardStats(testUser);

        // Then
        assertThat(stats.equityCurve()).hasSize(2); // Dwa dni: Jan 1 i Jan 2
        
        // Jan 1: 50 + 30 = 80
        assertThat(stats.equityCurve().get(0).date()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(stats.equityCurve().get(0).cumulativeProfit()).isEqualByComparingTo("80.00");
        
        // Jan 2: 80 - 40 = 40
        assertThat(stats.equityCurve().get(1).date()).isEqualTo(LocalDate.of(2023, 1, 2));
        assertThat(stats.equityCurve().get(1).cumulativeProfit()).isEqualByComparingTo("40.00");
    }

    @Test
    void getDashboardStats_ShouldReturnZeroROI_WhenNoBets() {
        // Given
        when(betRepository.findByUser(testUser)).thenReturn(Arrays.asList());

        // When
        DashboardStatsDTO stats = bettingService.getDashboardStats(testUser);

        // Then
        assertThat(stats.roi()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.equityCurve()).isEmpty();
    }

    @Test
    void getAdvancedStatistics_ShouldCalculateComplexMetrics() {
        // Given
        Bet win1 = new Bet();
        win1.setStatus(BetStatus.WON);
        win1.setStake(new BigDecimal("100"));
        win1.setFinalProfit(new BigDecimal("100"));
        win1.setSettledAt(LocalDate.of(2023, 1, 1).atStartOfDay());
        win1.setPlacedAt(LocalDate.of(2023, 1, 1).atStartOfDay());
        win1.setBetType(BetType.SINGLE);

        Bet win2 = new Bet();
        win2.setStatus(BetStatus.WON);
        win2.setStake(new BigDecimal("100"));
        win2.setFinalProfit(new BigDecimal("50"));
        win2.setSettledAt(LocalDate.of(2023, 1, 2).atStartOfDay());
        win2.setPlacedAt(LocalDate.of(2023, 1, 2).atStartOfDay());
        win2.setBetType(BetType.SINGLE);

        Bet loss = new Bet();
        loss.setStatus(BetStatus.LOST);
        loss.setStake(new BigDecimal("100"));
        loss.setFinalProfit(new BigDecimal("-100"));
        loss.setSettledAt(LocalDate.of(2023, 1, 3).atStartOfDay());
        loss.setPlacedAt(LocalDate.of(2023, 1, 3).atStartOfDay());
        loss.setBetType(BetType.SINGLE);

        when(betRepository.findByUserOrderByPlacedAtAsc(testUser)).thenReturn(Arrays.asList(win1, win2, loss));

        // When
        BetStatistics stats = bettingService.getAdvancedStatistics(testUser);

        // Then
        assertThat(stats.getTotalBets()).isEqualTo(3);
        assertThat(stats.getSuccessfulBets()).isEqualTo(2);
        assertThat(stats.getSharpeRatio()).isGreaterThan(BigDecimal.ZERO);
        assertThat(stats.getCurrentStreak()).contains("Current: 1 Loss");
    }
}