package com.grzechuhehe.SportsBettingManagerApp.repository;

import com.grzechuhehe.SportsBettingManagerApp.model.SofaScoreQueryCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SofaScoreQueryCacheRepository extends JpaRepository<SofaScoreQueryCache, Long> {

    Optional<SofaScoreQueryCache> findByQueryHash(String queryHash);

    List<SofaScoreQueryCache> findByQueryHashInAndExpiresAtAfter(
            Collection<String> queryHashes, LocalDateTime expiresAtAfter);

    void deleteByExpiresAtBefore(LocalDateTime expiresAtBefore);
}
