CREATE TABLE bet_resolution_attempt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bet_id BIGINT NOT NULL,
    cycle_id VARCHAR(36) NOT NULL,
    apify_data_available TINYINT(1) NOT NULL DEFAULT 0,
    match_found TINYINT(1) NOT NULL DEFAULT 0,
    match_confidence DOUBLE NULL,
    error_code VARCHAR(32) NULL,
    attempted_at DATETIME(6) NOT NULL,
    KEY idx_bet_id_attempted (bet_id, attempted_at),
    KEY idx_cycle_id (cycle_id)
);
