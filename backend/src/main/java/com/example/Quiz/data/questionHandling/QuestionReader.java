package com.example.Quiz.data.questionHandling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//QuestionReader contains the function readQuestions which takes a filepath and reads the Questions
//with the answers from that file
public class QuestionReader {

    public List<Question> readQuestions(String fileName) {
        List<Question> questions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            String currentQuestion = null;
            List<String> answers = new ArrayList<>();
            String correctAnswer = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                if (line.startsWith("Frage")) {
                    // Save the previous question before starting a new one
                    if (currentQuestion != null && !answers.isEmpty() && correctAnswer != null) {
                        questions.add(new Question(currentQuestion, new ArrayList<>(answers), correctAnswer));
                    }

                    // Start a new question
                    currentQuestion = reader.readLine(); // Read actual question text
                    answers.clear();
                    correctAnswer = null;
                } else {
                    // Read answer choices
                    if (line.endsWith("*")) {
                        correctAnswer = line.substring(0, line.length() - 1).trim(); // Remove the '*' and trim
                    }
                    answers.add(line);
                }
            }

            // Add the last question if valid
            if (currentQuestion != null && !answers.isEmpty() && correctAnswer != null) {
                questions.add(new Question(currentQuestion, answers, correctAnswer));
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        return questions;
    }
}
