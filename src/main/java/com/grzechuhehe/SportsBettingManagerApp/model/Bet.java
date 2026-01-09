package com.grzechuhehe.SportsBettingManagerApp.model;

import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.OddsType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@ToString(exclude = {"user", "parentBet", "childBets"}) // Zapobiega rekursywnemu wywoływaniu toString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Bet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Podstawowe informacje o zakładzie
    @NotNull
    @Enumerated(EnumType.STRING)
    private BetType betType; // SINGLE, PARLAY, SYSTEM

    @NotNull
    @Enumerated(EnumType.STRING)
    private BetStatus status; // PENDING, WON, LOST, etc.

    @NotNull
    @Positive(message = "Stake must be positive")
    @Column(precision = 10, scale = 2)
    private BigDecimal stake; // Stawka

    @NotNull
    @DecimalMin(value = "1.01", message = "Odds must be greater than 1.00")
    @Column(precision = 10, scale = 2)
    private BigDecimal odds; // Całkowity kurs zakładu

    @Enumerated(EnumType.STRING)
    private OddsType oddsType = OddsType.DECIMAL; // Format kursu (domyślnie dziesiętny)

    @Column(precision = 10, scale = 2)
    private BigDecimal potentialWinnings; // Potencjalna wygrana (stake * odds)

    @Column(precision = 10, scale = 2)
    private BigDecimal finalProfit; // Końcowy zysk/strata (może być ujemny)

    // Informacje o zdarzeniu
    private String sport; // Dyscyplina sportu, np. "Football"
    private String eventName; // Nazwa wydarzenia, np. "Real Madrid vs Barcelona"
    private LocalDateTime eventDate; // Data wydarzenia

    // Szczegóły wyboru
    @Enumerated(EnumType.STRING)
    private MarketType marketType; // Rodzaj rynku, np. MONEYLINE_1X2, TOTALS_OVER_UNDER
    private String selection; // Konkretny wybór, np. "Real Madrid", "Over 2.5"
    private String line; // Linia dla zakładów typu handicap lub over/under, np. "-1.5" lub "2.5"

    // Informacje o bukmacherze i API
    private String bookmaker; // Nazwa bukmachera
    private String externalBetId; // ID zakładu w systemie zewnętrznym (API)
    private String externalApiName; // Nazwa API, z którego pochodzi zakład

    // Daty
    @Column(nullable = false, updatable = false)
    private LocalDateTime placedAt = LocalDateTime.now(); // Data postawienia zakładu
    private LocalDateTime settledAt; // Data rozliczenia zakładu

    // Notatki użytkownika
    @Lob
    private String notes;

    // Relacje
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Hierarchia dla zakładów PARLAY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_bet_id")
    private Bet parentBet; // Zakład nadrzędny (dla "nóg" w PARLAY)

    @OneToMany(mappedBy = "parentBet", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Bet> childBets; // Lista "nóg" w zakładzie PARLAY

    // Metoda pomocnicza do obliczania potencjalnej wygranej
    @PrePersist
    @PreUpdate
    public void calculatePotentialWinnings() {
        if (stake != null && odds != null) {
            this.potentialWinnings = stake.multiply(odds);
        }
    }
}
