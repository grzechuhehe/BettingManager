package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.dto.BetRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.BetStatistics;
import com.grzechuhehe.SportsBettingManagerApp.dto.CreateBetRequest;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.grzechuhehe.SportsBettingManagerApp.dto.DashboardStatsDTO;

// ... existing imports

@Service
@RequiredArgsConstructor
public class BettingService {
    // ... existing fields

    public DashboardStatsDTO getDashboardStats(User user) {
        List<Bet> allBets = betRepository.findByUser(user).stream()
                .filter(b -> b.getParentBet() == null) // Tylko zakłady nadrzędne
                .collect(Collectors.toList());

        BigDecimal totalProfitLoss = allBets.stream()
                .filter(b -> b.getStatus() != BetStatus.PENDING)
                .map(b -> b.getFinalProfit() != null ? b.getFinalProfit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalBets = allBets.size();
        
        int activeBetsCount = (int) allBets.stream()
                .filter(b -> b.getStatus() == BetStatus.PENDING)
                .count();

        long wonBets = allBets.stream().filter(b -> b.getStatus() == BetStatus.WON).count();
        long lostBets = allBets.stream().filter(b -> b.getStatus() == BetStatus.LOST).count();
        long settledForWinRate = wonBets + lostBets;

        BigDecimal winRate = BigDecimal.ZERO;
        if (settledForWinRate > 0) {
            winRate = BigDecimal.valueOf(wonBets)
                    .divide(BigDecimal.valueOf(settledForWinRate), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return new DashboardStatsDTO(totalProfitLoss, totalBets, winRate, activeBetsCount);
    }
    
    @Transactional
    public List<Bet> placeBet(CreateBetRequest createBetRequest, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Bet> placedBets = new ArrayList<>();
        List<BetRequest> betRequests = createBetRequest.getBets();

        if (betRequests == null || betRequests.isEmpty()) {
            throw new IllegalArgumentException("Bet requests cannot be empty.");
        }

        if (betRequests.size() == 1) {
            // Single Bet
            BetRequest betRequest = betRequests.get(0);
            if (betRequest.getStake() == null || betRequest.getStake().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Stake must be positive for a single bet.");
            }
            Bet singleBet = buildBetFromRequest(betRequest, user, BetType.SINGLE, null, betRequest.getStake());
            placedBets.add(betRepository.save(singleBet));
        } else {
            // Parlay Bet
            BigDecimal parlayStake = betRequests.stream()
                    .map(BetRequest::getStake)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Stake is required for a parlay bet."));

            if (parlayStake.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Stake must be positive for a parlay bet.");
            }
            
            Bet parlayBet = Bet.builder()
                    .betType(BetType.PARLAY)
                    .status(BetStatus.PENDING)
                    .stake(parlayStake)
                    .odds(calculateParlayOdds(betRequests))
                    .user(user)
                    .placedAt(LocalDateTime.now())
                    .sport("Multi-Sport")
                    .eventName("Parlay Bet (" + betRequests.size() + " legs)")
                    .build();

            Set<Bet> childBets = new HashSet<>();
            for (BetRequest betRequest : betRequests) {
                // Child bets have a null stake as it's owned by the parent parlay bet
                Bet childBet = buildBetFromRequest(betRequest, user, BetType.SINGLE, parlayBet, null);
                childBets.add(childBet);
            }
            parlayBet.setChildBets(childBets);

            // Save the parent bet, which cascades to save the child bets
            Bet savedParlayBet = betRepository.save(parlayBet);
            placedBets.add(savedParlayBet);
        }
        return placedBets;
    }

    private Bet buildBetFromRequest(BetRequest betRequest, User user, BetType betType, Bet parentBet, BigDecimal stake) {
        Bet bet = Bet.builder()
                .betType(betType)
                .status(BetStatus.PENDING)
                .stake(stake)
                .odds(betRequest.getOdds())
                .oddsType(betRequest.getOddsType() != null ? betRequest.getOddsType() : com.grzechuhehe.SportsBettingManagerApp.model.enum_model.OddsType.DECIMAL)
                .sport(betRequest.getSport())
                .eventName(betRequest.getEventName())
                .eventDate(betRequest.getEventDate())
                .marketType(betRequest.getMarketType())
                .selection(betRequest.getSelection())
                .line(betRequest.getLine())
                .bookmaker(betRequest.getBookmaker())
                .externalBetId(betRequest.getExternalBetId())
                .externalApiName(betRequest.getExternalApiName())
                .notes(betRequest.getNotes())
                .user(user)
                .placedAt(LocalDateTime.now())
                .parentBet(parentBet)
                .build();
        bet.calculatePotentialWinnings();
        return bet;
    }

    private BigDecimal calculateParlayOdds(List<BetRequest> bets) {
        return bets.stream()
                .map(BetRequest::getOdds)
                .reduce(BigDecimal.ONE, BigDecimal::multiply)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public List<Bet> getUserBets(User user) {
        // Filtrujemy, aby nie pokazywać "nóg" jako zakłady najwyższego poziomu
        return betRepository.findByUser(user).stream()
                .filter(bet -> bet.getParentBet() == null)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getStatistics(User user) {
        List<Bet> bets = betRepository.findByUser(user).stream()
                .filter(b -> b.getParentBet() == null) // Statystyki tylko dla zakładów nadrzędnych
                .collect(Collectors.toList());

        BigDecimal totalStake = bets.stream()
                .map(Bet::getStake)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profitLoss = calculateNetProfit(bets);
        BigDecimal roi = calculateROI(profitLoss, totalStake);

        List<Bet> recentBets = bets.stream()
                .sorted(Comparator.comparing(Bet::getPlacedAt).reversed())
                .limit(5)
                .collect(Collectors.toList());

        return Map.of(
                "totalBets", bets.size(),
                "wonBets", betRepository.countByUserAndStatus(user, BetStatus.WON),
                "totalStake", totalStake,
                "profitLoss", profitLoss,
                "roi", roi,
                "recentBets", recentBets
        );
    }

    public BetStatistics getAdvancedStatistics(User user) {
        List<Bet> bets = betRepository.findByUserOrderByPlacedAtAsc(user).stream()
                .filter(b -> b.getParentBet() == null) // Statystyki tylko dla zakładów nadrzędnych
                .collect(Collectors.toList());

        BigDecimal totalInvestment = bets.stream()
                .map(Bet::getStake)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profit = calculateNetProfit(bets);
        BigDecimal roi = calculateROI(profit, totalInvestment);

        return new BetStatistics(
                bets.size(),
                countSuccessfulBets(bets),
                profit,
                roi,
                calculateWinRatesByType(bets),
                calculateRollingAverage(bets, 30),
                analyzeStreaks(bets)
        );
    }

    private int countSuccessfulBets(List<Bet> bets) {
        return (int) bets.stream()
                .filter(bet -> bet.getStatus() == BetStatus.WON)
                .count();
    }

    private BigDecimal calculateNetProfit(List<Bet> bets) {
        return bets.stream()
                .filter(bet -> bet.getStatus() != BetStatus.PENDING) // Uwzględnij tylko rozliczone zakłady
                .map(bet -> Optional.ofNullable(bet.getFinalProfit()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateROI(BigDecimal profit, BigDecimal investment) {
        if (investment.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return profit.divide(investment, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private Map<String, BigDecimal> calculateWinRatesByType(List<Bet> bets) {
        return bets.stream()
                .collect(Collectors.groupingBy(
                        bet -> bet.getBetType().name(), // Użyj .name() aby uzyskać String z enuma
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    long total = list.size();
                                    if (total == 0) return BigDecimal.ZERO;
                                    long won = list.stream()
                                            .filter(b -> b.getStatus() == BetStatus.WON)
                                            .count();
                                    return BigDecimal.valueOf(won)
                                            .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                                            .multiply(BigDecimal.valueOf(100));
                                }
                        )
                ));
    }
    
    private BigDecimal calculateRollingAverage(List<Bet> bets, int days) {
        LocalDate cutoff = LocalDate.now().minusDays(days);
    
        BigDecimal periodProfit = bets.stream()
                .filter(b -> b.getPlacedAt().isAfter(cutoff.atStartOfDay()))
                .filter(bet -> bet.getStatus() != BetStatus.PENDING)
                .map(bet -> Optional.ofNullable(bet.getFinalProfit()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    
        return periodProfit.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    private String analyzeStreaks(List<Bet> bets) {
        int currentStreak = 0;
        int maxWinStreak = 0;
        int maxLossStreak = 0;
        BetStatus lastStatus = null;
        int streakCounter = 0;

        List<Bet> settledBets = bets.stream()
                .filter(b -> b.getStatus() == BetStatus.WON || b.getStatus() == BetStatus.LOST)
                .sorted(Comparator.comparing(Bet::getSettledAt))
                .collect(Collectors.toList());

        for (Bet bet : settledBets) {
            if (lastStatus == null || bet.getStatus() != lastStatus) {
                lastStatus = bet.getStatus();
                streakCounter = 1;
            } else {
                streakCounter++;
            }
            if (lastStatus == BetStatus.WON && streakCounter > maxWinStreak) {
                maxWinStreak = streakCounter;
            } else if (lastStatus == BetStatus.LOST && streakCounter > maxLossStreak) {
                maxLossStreak = streakCounter;
            }
        }
        
        BetStatus currentStatus = settledBets.isEmpty() ? null : settledBets.get(settledBets.size()-1).getStatus();
        if(currentStatus != null){
             long reversedStreak = 0;
             for(int i = settledBets.size()-1; i>=0; i--){
                if(settledBets.get(i).getStatus() == currentStatus) reversedStreak++;
                else break;
             }
             return String.format("Current: %d %s, Max Win: %d, Max Loss: %d", reversedStreak, currentStatus, maxWinStreak, maxLossStreak);
        }
        
        return "No active streak";
    }

    // ... reszta metod statystycznych, jeśli wymagają poprawek ...
    // Zakładam, że sharpeRatio i heatmapData są bardziej złożone i na razie skupiamy się na podstawowych statystykach

    public Map<String, Map<String, Double>> getHeatmapData(User user) {
        List<Bet> bets = betRepository.findByUser(user);

        return bets.stream()
                .filter(b -> b.getParentBet() == null)
                .collect(Collectors.groupingBy(
                        b -> b.getPlacedAt().getDayOfWeek().toString(),
                        Collectors.groupingBy(
                                b -> b.getBetType().toString(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> {
                                            long total = list.size();
                                            if (total == 0) return 0.0;
                                            long won = list.stream().filter(b -> b.getStatus() == BetStatus.WON).count();
                                            return (double) won / total * 100;
                                        }
                                )
                        )
                ));
    }

    @Transactional
    public Bet settleBet(Long betId, BetStatus newStatus, User user) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new IllegalArgumentException("Bet not found with id: " + betId));

        if (!bet.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to settle this bet.");
        }

        if (bet.getStatus() != BetStatus.PENDING) {
            throw new IllegalArgumentException("Bet is already settled.");
        }

        bet.setStatus(newStatus);
        bet.setSettledAt(LocalDateTime.now());

        // Obliczanie finalProfit
        if (newStatus == BetStatus.WON) {
            // Zysk netto = (stawka * kurs) - stawka
            bet.setFinalProfit(bet.getPotentialWinnings().subtract(bet.getStake()));
        } else if (newStatus == BetStatus.LOST) {
            // Strata = -stawka
            bet.setFinalProfit(bet.getStake().negate());
        } else if (newStatus == BetStatus.VOID) {
            // Zwrot = 0
            bet.setFinalProfit(BigDecimal.ZERO);
        }

        // Propagacja statusu na "nogi" (legs) dla Parlay
        if (bet.getBetType() == BetType.PARLAY && bet.getChildBets() != null) {
            for (Bet child : bet.getChildBets()) {
                child.setStatus(newStatus);
                child.setSettledAt(bet.getSettledAt());
            }
        }

        return betRepository.save(bet);
    }
}



