package com.example.Quiz.data.questionHandling;

import java.util.List;

public class Question {

    private final String question;
    private final List<String> answers;
    private final String correctAnswer;

    public Question(String question, List<String> answers, String correctAnswer) {
        this.question = question;
        this.answers = answers;
        this.correctAnswer = correctAnswer;
    }

    // Getters if you want to access them in other classes
    public String getQuestion() {
        return question;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }
}
