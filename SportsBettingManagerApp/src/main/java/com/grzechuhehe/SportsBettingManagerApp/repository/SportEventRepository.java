package com.grzechuhehe.SportsBettingManagerApp.repository;

import com.grzechuhehe.SportsBettingManagerApp.model.SportEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SportEventRepository extends JpaRepository<SportEvent, Long> {
}