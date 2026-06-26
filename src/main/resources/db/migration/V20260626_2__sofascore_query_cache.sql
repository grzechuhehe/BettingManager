CREATE TABLE sofascore_query_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query_hash CHAR(64) NOT NULL,
    query_text VARCHAR(512) NOT NULL,
    payload_json MEDIUMTEXT NOT NULL,
    event_count INT NOT NULL DEFAULT 0,
    fetched_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'APIFY',
    UNIQUE KEY uk_query_hash (query_hash),
    KEY idx_expires_at (expires_at)
);
