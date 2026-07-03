-- Supporting queries by attempted_at (health monitor) and error_code + attempted_at (success counts).
ALTER TABLE bet_resolution_attempt
    ADD KEY idx_attempted_at (attempted_at),
    ADD KEY idx_error_code_attempted (error_code, attempted_at);
