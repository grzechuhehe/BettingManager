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
import com.grzechuhehe.SportsBettingManagerApp.dto.EquityCurvePoint;

@Service
@RequiredArgsConstructor
public class BettingService {

    private final BetRepository betRepository;
    private final UserRepository userRepository;


    public DashboardStatsDTO getDashboardStats(User user) {
        
        List<Bet> allBets = betRepository.findByUser(user).stream()
                .filter(b -> b.getParentBet() == null) // Tylko zakłady nadrzędne
                .collect(Collectors.toList());

        // Podstawowe statystyki
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

        // Zaawansowane statystyki (Bloomberg style)
        BigDecimal totalStaked = allBets.stream()
                .map(Bet::getStake)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal roi = BigDecimal.ZERO;
        BigDecimal yield = BigDecimal.ZERO;

        if (totalStaked.compareTo(BigDecimal.ZERO) > 0) {
            roi = totalProfitLoss.divide(totalStaked, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            // Yield w bukmacherce to często to samo co ROI (zysk/obrót), ale czasem liczone inaczej. Przyjmijmy definicję Zysk Netto / Obrót.
            yield = roi; 
        }

        // Profit by Sport
        Map<String, BigDecimal> profitBySport = allBets.stream()
                .filter(b -> b.getStatus() != BetStatus.PENDING && b.getSport() != null)
                .collect(Collectors.groupingBy(
                        Bet::getSport,
                        Collectors.mapping(
                                b -> Optional.ofNullable(b.getFinalProfit()).orElse(BigDecimal.ZERO),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        // Equity Curve (Skumulowany zysk w czasie)
        List<Bet> sortedSettledBets = allBets.stream()
                .filter(b -> b.getStatus() != BetStatus.PENDING && b.getSettledAt() != null)
                .sorted(Comparator.comparing(Bet::getSettledAt))
                .collect(Collectors.toList());

        List<EquityCurvePoint> equityCurve = new ArrayList<>();
        BigDecimal currentEquity = BigDecimal.ZERO;

        // Dodajemy punkt startowy (0, 0) - opcjonalne, ale ładnie wygląda na wykresie
        // equityCurve.add(new EquityCurvePoint(LocalDate.now(), BigDecimal.ZERO)); 

        // Grupowanie dzienne dla czytelności wykresu
        Map<LocalDate, BigDecimal> dailyProfits = new TreeMap<>(); // TreeMap trzyma kolejność dat

        for (Bet bet : sortedSettledBets) {
            LocalDate date = bet.getSettledAt().toLocalDate();
            BigDecimal profit = Optional.ofNullable(bet.getFinalProfit()).orElse(BigDecimal.ZERO);
            dailyProfits.merge(date, profit, BigDecimal::add);
        }

        for (Map.Entry<LocalDate, BigDecimal> entry : dailyProfits.entrySet()) {
            currentEquity = currentEquity.add(entry.getValue());
            equityCurve.add(new EquityCurvePoint(entry.getKey(), currentEquity));
        }

        return new DashboardStatsDTO(
                totalProfitLoss, 
                totalBets, 
                winRate, 
                activeBetsCount,
                roi,
                yield,
                totalStaked,
                profitBySport,
                equityCurve
        );
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
                analyzeStreaks(bets),
                calculateSharpeRatio(bets)
        );
    }

    private BigDecimal calculateSharpeRatio(List<Bet> bets) {
        List<BigDecimal> profits = bets.stream()
                .filter(b -> b.getStatus() != BetStatus.PENDING && b.getFinalProfit() != null)
                .map(Bet::getFinalProfit)
                .collect(Collectors.toList());

        if (profits.size() < 2) return BigDecimal.ZERO;

        // 1. Średni zysk (Mean Return)
        BigDecimal sum = profits.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(profits.size()), 4, RoundingMode.HALF_UP);

        // 2. Odchylenie standardowe (Standard Deviation)
        BigDecimal varianceSum = BigDecimal.ZERO;
        for (BigDecimal profit : profits) {
            BigDecimal diff = profit.subtract(mean);
            varianceSum = varianceSum.add(diff.pow(2));
        }
        
        // Dzielimy przez N (dla populacji) lub N-1 (dla próbki). Tutaj N dla uproszczenia.
        BigDecimal variance = varianceSum.divide(BigDecimal.valueOf(profits.size()), 4, RoundingMode.HALF_UP);
        double stdDevDouble = Math.sqrt(variance.doubleValue());
        BigDecimal stdDev = BigDecimal.valueOf(stdDevDouble);

        if (stdDev.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        // 3. Sharpe Ratio = Mean / StdDev (zakładając Risk Free Rate = 0)
        return mean.divide(stdDev, 4, RoundingMode.HALF_UP);
    }

    private int countSuccessfulBets(List<Bet> bets) {
        return (int) bets.stream()
                .filter(bet -> bet.getStatus() == BetStatus.WON)
                .count();
    }

    private BigDecimal calculateNetProfit(List<Bet> bets) {
        return bets.stream()
                .filter(bet -> bet.getStatus() != BetStatus.PENDING)
                .map(bet -> Optional.ofNullable(bet.getFinalProfit()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateROI(BigDecimal profit, BigDecimal investment) {
        if (investment == null || investment.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return profit.divide(investment, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private Map<String, BigDecimal> calculateWinRatesByType(List<Bet> bets) {
        return bets.stream()
                .collect(Collectors.groupingBy(
                        bet -> bet.getBetType().name(),
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
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

        BigDecimal periodProfit = bets.stream()
                .filter(b -> b.getPlacedAt().isAfter(cutoff))
                .filter(bet -> bet.getStatus() != BetStatus.PENDING)
                .map(bet -> Optional.ofNullable(bet.getFinalProfit()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return periodProfit.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    private String analyzeStreaks(List<Bet> bets) {
        int maxWinStreak = 0;
        int maxLossStreak = 0;
        BetStatus lastStatus = null;
        int streakCounter = 0;

        // Sortujemy po dacie rozliczenia, żeby analiza miała sens chronologiczny
        List<Bet> settledBets = bets.stream()
                .filter(b -> b.getStatus() == BetStatus.WON || b.getStatus() == BetStatus.LOST)
                .filter(b -> b.getSettledAt() != null)
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
        
        // Obliczanie bieżącej serii (od tyłu)
        int currentStreakCount = 0;
        String currentStreakType = "None";
        
        if (!settledBets.isEmpty()) {
            Bet lastBet = settledBets.get(settledBets.size() - 1);
            BetStatus currentStatus = lastBet.getStatus();
            currentStreakType = currentStatus == BetStatus.WON ? "Win" : "Loss";
            
            for (int i = settledBets.size() - 1; i >= 0; i--) {
                if (settledBets.get(i).getStatus() == currentStatus) {
                    currentStreakCount++;
                } else {
                    break;
                }
            }
        }
        
        return String.format("Current: %d %s | Max Win: %d | Max Loss: %d", 
                currentStreakCount, currentStreakType, maxWinStreak, maxLossStreak);
    }

    // Zmieniona sygnatura i logika: Data (String YYYY-MM-DD) -> Zysk (BigDecimal)
    public Map<String, BigDecimal> getHeatmapData(User user) {
        List<Bet> bets = betRepository.findByUser(user).stream()
                .filter(b -> b.getParentBet() == null)
                .filter(b -> b.getStatus() != BetStatus.PENDING) // Tylko rozliczone
                .collect(Collectors.toList());

        // Grupujemy po dacie rozliczenia (SettledAt) lub postawienia (PlacedAt). 
        // Dla heatmapy zysków lepiej użyć daty rozliczenia (kiedy zysk faktycznie powstał).
        return bets.stream()
                .filter(b -> b.getSettledAt() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getSettledAt().toLocalDate().toString(),
                        Collectors.mapping(
                                b -> Optional.ofNullable(b.getFinalProfit()).orElse(BigDecimal.ZERO),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
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

    @Transactional
    public Bet updateBet(Long betId, BetRequest betRequest, User user) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new IllegalArgumentException("Bet not found with id: " + betId));

        if (!bet.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to update this bet.");
        }

        // Allow updates mostly for PENDING bets, but let's allow flexibility as requested
        // Mapping fields from request to entity
        bet.setSport(betRequest.getSport());
        bet.setEventName(betRequest.getEventName());
        bet.setEventDate(betRequest.getEventDate());
        bet.setMarketType(betRequest.getMarketType());
        bet.setSelection(betRequest.getSelection());
        bet.setOdds(betRequest.getOdds());
        bet.setBookmaker(betRequest.getBookmaker());
        bet.setStake(betRequest.getStake()); // Important: Stake change affects potential winnings
        bet.setNotes(betRequest.getNotes());
        
        // Recalculate potential winnings if stake or odds changed
        bet.calculatePotentialWinnings();

        return betRepository.save(bet);
    }

    @Transactional
    public void deleteBet(Long betId, User user) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new IllegalArgumentException("Bet not found with id: " + betId));

        if (!bet.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to delete this bet.");
        }
        
        betRepository.delete(bet);
    }
}



