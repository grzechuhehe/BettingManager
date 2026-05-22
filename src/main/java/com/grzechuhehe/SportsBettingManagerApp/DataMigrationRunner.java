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
        List<User> shadowProfiles = userRepository.findAll().stream()
                .filter(u -> !u.isActiveUser() && u.getXUsername() == null)
                .toList();

        for (User user : shadowProfiles) {
            user.setXUsername(user.getUsername());
            userRepository.save(user);
        }
    }
}
