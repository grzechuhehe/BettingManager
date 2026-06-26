package com.grzechuhehe.SportsBettingManagerApp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sofascore_query_cache")
@Getter
@Setter
public class SofaScoreQueryCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_hash", nullable = false, length = 64)
    private String queryHash;

    @Column(name = "query_text", nullable = false, length = 512)
    private String queryText;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    private int eventCount;

    private LocalDateTime fetchedAt;

    private LocalDateTime expiresAt;

    private String source = "APIFY";
}
