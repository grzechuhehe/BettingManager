package com.grzechuhehe.SportsBettingManagerApp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SportEvent {
    public enum SportType {FOOTBALL, TENIS, ICEHOKEY, FORMULA1, HANDBALL, AMERICANFOOTBALL, VOLLEYBALL, BASKETBALL, MMA, BOXING, CS, LOL, POLITICS, SHOWBIZNES }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String teamHome;
    private String teamAway;
    private LocalDateTime date;
    private SportType sportType;
    private int betCount = 0;
}

