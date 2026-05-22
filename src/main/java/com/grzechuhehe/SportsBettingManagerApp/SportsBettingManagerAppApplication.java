package com.grzechuhehe.SportsBettingManagerApp;

import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootApplication
@EnableScheduling
public class SportsBettingManagerAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(SportsBettingManagerAppApplication.class, args);
	}

	@Bean
	@Transactional
	public CommandLineRunner migrateShadowProfiles(UserRepository userRepository) {
		return args -> {
			List<User> shadowProfiles = userRepository.findAll().stream()
					.filter(u -> !u.isActiveUser() && u.getXUsername() == null)
					.toList();
			
			for (User user : shadowProfiles) {
				user.setXUsername(user.getUsername());
				userRepository.save(user);
			}
		};
	}
}
