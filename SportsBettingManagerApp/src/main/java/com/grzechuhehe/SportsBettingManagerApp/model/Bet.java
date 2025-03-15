package com.grzechuhehe.SportsBettingManagerApp.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Bet {
    public enum BetType { WIN, DRAW, OVER, UNDER, }
    public enum BetStatus { PENDING, WON, LOST }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    @NotNull
    @DecimalMin("1.01")
    private BigDecimal odds;
    private BetType type;
    private BetStatus status;
    @Column(name = "placed_at")
    private LocalDateTime placedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private SportEvent event;

}

