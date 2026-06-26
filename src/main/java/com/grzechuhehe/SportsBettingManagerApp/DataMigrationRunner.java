package com.grzechuhehe.SportsBettingManagerApp;

import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataMigrationRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataMigrationRunner(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Migracja 1: Ustawienie xUsername dla starych shadow profilów
        List<User> shadowProfiles = userRepository.findAll().stream()
                .filter(u -> !u.isActiveUser() && u.getXUsername() == null)
                .toList();

        for (User user : shadowProfiles) {
            user.setXUsername(user.getUsername());
            userRepository.save(user);
        }

        // Migracja 2: Ustawienie domyślnego progu EV dla istniejących użytkowników
        List<User> usersWithNullThreshold = userRepository.findAll().stream()
                .filter(u -> u.getEvEdgeThreshold() == null)
                .toList();
        
        if (!usersWithNullThreshold.isEmpty()) {
            for (User user : usersWithNullThreshold) {
                user.setEvEdgeThreshold(2);
                userRepository.save(user);
            }
        }
    }
}
