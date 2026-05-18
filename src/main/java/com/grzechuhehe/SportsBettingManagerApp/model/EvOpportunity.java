package com.grzechuhehe.SportsBettingManagerApp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
public class EvOpportunity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String eventName;
    private String bookmaker;
    private BigDecimal bookmakerOdds;
    private BigDecimal trueProbability;
    private BigDecimal evPercentage;
    private LocalDateTime detectedAt;
}
