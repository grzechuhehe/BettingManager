package com.grzechuhehe.SportsBettingManagerApp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bet_resolution_attempt")
@Getter
@Setter
public class BetResolutionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bet_id", nullable = false)
    private Long betId;

    @Column(name = "cycle_id", nullable = false, length = 36)
    private String cycleId;

    @Column(name = "apify_data_available", nullable = false)
    private boolean apifyDataAvailable;

    @Column(name = "match_found", nullable = false)
    private boolean matchFound;

    private Double matchConfidence;

    @Column(name = "error_code", length = 32)
    private String errorCode;

    @Column(name = "phase", length = 16)
    private String phase;

    @Column(name = "enrichment_attempted", nullable = false)
    private boolean enrichmentAttempted;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;
}
