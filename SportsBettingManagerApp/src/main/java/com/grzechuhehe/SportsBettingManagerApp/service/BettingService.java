package com.grzechuhehe.SportsBettingManagerApp.service;

import com.grzechuhehe.SportsBettingManagerApp.dto.BetStatistics;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.SportEvent;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.SportEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BettingService {
    private final BetRepository betRepository;
    private final com.grzechuhehe.SportsBettingManagerApp.repository.SportEventRepository sportEventRepository;

    public Bet placeBet(Bet bet) {
        SportEvent incomingEvent = bet.getEvent();
        SportEvent savedEvent;

        if (incomingEvent == null) {
            throw new IllegalArgumentException("Bet must be associated with a SportEvent.");
        }

        // Spróbuj znaleźć istniejące wydarzenie sportowe
        java.util.Optional<SportEvent> existingEvent = sportEventRepository.findByTeamHomeAndTeamAwayAndDateAndSportType(
                incomingEvent.getTeamHome(),
                incomingEvent.getTeamAway(),
                incomingEvent.getDate(),
                incomingEvent.getSportType()
        );

        if (existingEvent.isPresent()) {
            savedEvent = existingEvent.get();
            org.slf4j.LoggerFactory.getLogger(BettingService.class).info("Znaleziono istniejące wydarzenie sportowe: {}", savedEvent.getId());
        } else {
            // Nowe wydarzenie sportowe - zapisz je
            org.slf4j.LoggerFactory.getLogger(BettingService.class).info("Zapisuję nowe wydarzenie sportowe: {}", incomingEvent);
            savedEvent = sportEventRepository.save(incomingEvent);
        }

        // Zwiększ licznik zakładów dla wydarzenia
        savedEvent.setBetCount(savedEvent.getBetCount() + 1);
        sportEventRepository.save(savedEvent); // Zapisz zaktualizowane wydarzenie

        // Ustaw zaktualizowane wydarzenie w zakładzie
        bet.setEvent(savedEvent);

        // Skopiuj nazwy drużyn z wydarzenia do zakładu
        bet.setTeamHome(savedEvent.getTeamHome());
        bet.setTeamAway(savedEvent.getTeamAway());
        
        // Teraz możemy zapisać zakład
        return betRepository.save(bet);
    }

    public List<Bet> getUserBets(User user) {
        return betRepository.findByUser(user);
    }

    public Map<String, Object> getStatistics(User user) {
        List<Bet> bets = betRepository.findByUser(user);

        BigDecimal totalAmount = bets.stream()
                .map(Bet::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profitLoss = bets.stream()
                .map(b -> b.getStatus() == Bet.BetStatus.WON ?
                        b.getAmount().multiply(b.getOdds().subtract(BigDecimal.ONE)) :
                        b.getStatus() == Bet.BetStatus.LOST ? b.getAmount().negate() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Oblicz ROI tylko jeśli totalAmount > 0
        BigDecimal roi = totalAmount.compareTo(BigDecimal.ZERO) > 0 ?
                profitLoss.divide(totalAmount, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Ostatnie zakłady
        List<Bet> recentBets = bets.stream()
                .sorted((b1, b2) -> b2.getPlacedAt().compareTo(b1.getPlacedAt()))
                .limit(5)
                .collect(Collectors.toList());

        return Map.of(
                "totalBets", bets.size(),
                "wonBets", betRepository.countByUserAndStatus(user, Bet.BetStatus.WON),
                "totalAmount", totalAmount,
                "profitLoss", profitLoss,
                "roi", roi,
                "recentBets", recentBets
        );
    }

    public BetStatistics getAdvancedStatistics(User user) {
        List<Bet> bets = betRepository.findByUserOrderByPlacedAtAsc(user);

        BigDecimal totalInvestment = bets.stream()
                .map(Bet::getAmount)
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
                .filter(bet -> bet.getStatus() == Bet.BetStatus.WON)
                .count();
    }

    private BigDecimal calculateNetProfit(List<Bet> bets) {
        return bets.stream()
                .map(bet -> {
                    if (bet.getStatus() == Bet.BetStatus.WON) {
                        return bet.getAmount().multiply(bet.getOdds().subtract(BigDecimal.ONE));
                    } else if (bet.getStatus() == Bet.BetStatus.LOST) {
                        return bet.getAmount().negate();
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateROI(BigDecimal profit, BigDecimal investment) {
        if (investment.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return profit.divide(investment, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private Map<Bet.BetType, BigDecimal> calculateWinRatesByType(List<Bet> bets) {
        return bets.stream()
                .collect(Collectors.groupingBy(
                        Bet::getType,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    long total = list.size();
                                    long won = list.stream()
                                            .filter(b -> b.getStatus() == Bet.BetStatus.WON)
                                            .count();
                                    return BigDecimal.valueOf((double) won / total * 100)
                                            .setScale(2, RoundingMode.HALF_UP);
                                }
                        )
                ));
    }

    private BigDecimal calculateRollingAverage(List<Bet> bets, int days) {
        LocalDate cutoff = LocalDate.now().minusDays(days);

        BigDecimal periodProfit = bets.stream()
                .filter(b -> b.getPlacedAt().isAfter(cutoff.atStartOfDay()))
                .map(b -> b.getStatus() == Bet.BetStatus.WON ?
                        b.getAmount().multiply(b.getOdds()) :
                        b.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return periodProfit.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    private String analyzeStreaks(List<Bet> bets) {
        int currentStreak = 0;
        Bet.BetStatus lastStatus = null;

        for (Bet bet : bets) {
            if (bet.getStatus() != Bet.BetStatus.PENDING) {
                if (lastStatus == null) {
                    lastStatus = bet.getStatus();
                    currentStreak = 1;
                } else if (bet.getStatus() == lastStatus) {
                    currentStreak++;
                } else {
                    break;
                }
            }
        }

        return currentStreak > 0 ?
                currentStreak + " " + lastStatus.toString() + " streak" :
                "No active streak";
    }

    private BigDecimal calculateSharpeRatio(List<Bet> bets) {
        List<BigDecimal> returns = bets.stream()
                .filter(b -> b.getStatus() != Bet.BetStatus.PENDING)
                .map(b -> b.getStatus() == Bet.BetStatus.WON ?
                        b.getAmount().multiply(b.getOdds().subtract(BigDecimal.ONE)) :
                        b.getAmount().negate())
                .collect(Collectors.toList());

        if (returns.isEmpty()) return BigDecimal.ZERO;

        BigDecimal avgReturn = calculateAverage(returns);
        BigDecimal stdDev = calculateStandardDeviation(returns, avgReturn);

        return stdDev.compareTo(BigDecimal.ZERO) != 0 ?
                avgReturn.divide(stdDev, 4, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
    }

    public Map<String, Map<String, Double>> getHeatmapData(User user) {
        List<Bet> bets = betRepository.findByUser(user);

        return bets.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getPlacedAt().getDayOfWeek().toString(),
                        Collectors.groupingBy(
                                b -> b.getType().toString(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> {
                                            long total = list.size();
                                            long won = list.stream().filter(b -> b.getStatus() == Bet.BetStatus.WON).count();
                                            return total > 0 ? (double) won / total * 100 : 0.0;
                                        }
                                )
                        )
                ));
    }

    private BigDecimal calculateAverage(List<BigDecimal> values) {
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStandardDeviation(List<BigDecimal> values, BigDecimal mean) {
        BigDecimal variance = values.stream()
                .map(val -> val.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);

        return new BigDecimal(Math.sqrt(variance.doubleValue()));
    }


}

