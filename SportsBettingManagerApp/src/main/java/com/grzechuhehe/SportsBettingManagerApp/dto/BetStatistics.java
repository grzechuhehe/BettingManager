package com.grzechuhehe.SportsBettingManagerApp.dto;

import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BetStatistics {
    private int totalBets;
    private int successfulBets;
    private BigDecimal profitLoss;
    private BigDecimal roiPercentage;
    private Map<Bet.BetType, BigDecimal> winRatesByType;
    private BigDecimal rollingAverage30d;
    private String currentStreak;
    private BigDecimal sharpeRatio;

    public BetStatistics(int totalBets, int successfulBets, BigDecimal profitLoss, BigDecimal roiPercentage,
                         Map<Bet.BetType, BigDecimal> winRatesByType, BigDecimal rollingAverage30d, String currentStreak) {
        this.totalBets = totalBets;
        this.successfulBets = successfulBets;
        this.profitLoss = profitLoss;
        this.roiPercentage = roiPercentage;
        this.winRatesByType = winRatesByType;
        this.rollingAverage30d = rollingAverage30d;
        this.currentStreak = currentStreak;
    }
}