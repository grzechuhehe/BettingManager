package com.grzechuhehe.SportsBettingManagerApp.repository;

import com.grzechuhehe.SportsBettingManagerApp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.xUsername IS NOT NULL AND (u.lastXCheckAt IS NULL OR u.lastXCheckAt < :threshold)")
    List<User> findProfilesToUpdate(@Param("threshold") LocalDateTime threshold);
}