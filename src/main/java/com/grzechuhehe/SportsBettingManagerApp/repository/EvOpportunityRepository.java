package com.grzechuhehe.SportsBettingManagerApp.repository;

import com.grzechuhehe.SportsBettingManagerApp.model.EvOpportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EvOpportunityRepository extends JpaRepository<EvOpportunity, Long> {
    
    @Query("SELECT e FROM EvOpportunity e WHERE e.detectedAt > :since AND e.evPercentage >= :minEdge AND e.id IN (" +
           "SELECT MAX(e2.id) FROM EvOpportunity e2 GROUP BY e2.eventName, e2.targetSelection, e2.bookmaker" +
           ") ORDER BY e.evPercentage DESC")
    List<EvOpportunity> findLatestUniqueOpportunitiesFiltered(@Param("since") LocalDateTime since, @Param("minEdge") Double minEdge);

    List<EvOpportunity> findByDetectedAtAfterOrderByEvPercentageDesc(LocalDateTime detectedAt);
}
