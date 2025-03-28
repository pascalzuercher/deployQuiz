package com.example.Quiz.data.questionHandling;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class QuestionReader {

    public List<Question> readQuestions(String resourcePath) {
        List<Question> questions = new ArrayList<>();

        InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            System.err.println("❌ Datei nicht gefunden: " + resourcePath);
            return questions;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            String currentQuestion = null;
            List<String> answers = new ArrayList<>();
            String correctAnswer = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Überspringe Kommentare & leere Zeilen
                }

                // Wenn die aktuelle Frage noch nicht gesetzt ist
                if (currentQuestion == null) {
                    currentQuestion = line;
                    answers.clear();
                    correctAnswer = null;
                } else if (line.matches("^[A-C]\\*?\\s.*")) {
                    // Antwortzeile beginnt mit A/B/C (optional mit *)
                    String answerText = line.substring(2).trim();
                    if (line.startsWith("A*") || line.startsWith("B*") || line.startsWith("C*")) {
                        correctAnswer = answerText;
                    }
                    answers.add(answerText);
                }

                // Wenn 3 Antworten gesammelt wurden
                if (answers.size() == 3 && correctAnswer != null) {
                    questions.add(new Question(currentQuestion, new ArrayList<>(answers), correctAnswer));
                    currentQuestion = null;
                    answers.clear();
                    correctAnswer = null;
                }
            }

        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der Datei: " + e.getMessage());
        }

        return questions;
    }
}
