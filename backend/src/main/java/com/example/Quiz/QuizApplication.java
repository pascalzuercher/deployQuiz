package com.example.Quiz;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class QuizApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuizApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner() {
		return args -> {
			System.out.println("\n📘 Quiz-Server erfolgreich gestartet!");
			System.out.println("Trete der Lobby unter: http://localhost:8080 bei. Viel Spass!");
		};
	}
}
