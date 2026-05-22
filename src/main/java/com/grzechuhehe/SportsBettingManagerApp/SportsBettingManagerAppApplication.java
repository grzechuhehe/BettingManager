package com.grzechuhehe.SportsBettingManagerApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SportsBettingManagerAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(SportsBettingManagerAppApplication.class, args);
	}

}
