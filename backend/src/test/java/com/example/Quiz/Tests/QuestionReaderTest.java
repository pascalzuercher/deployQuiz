package com.example.Quiz.Tests;

import com.example.Quiz.data.questionHandling.Question;
import com.example.Quiz.data.questionHandling.QuestionReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class QuestionReaderTest {

    private QuestionReader questionReader;

    @BeforeEach
    void setUp() {
        questionReader = new QuestionReader();
    }

    @Test
    void testSuccessfulQuestionParsing() {
        // Note the full path to the resource file
        List<Question> questions = questionReader.readQuestions("test-questions-valid.txt");

        assertFalse(questions.isEmpty(), "Questions should be parsed successfully");

        // Verify first question details
        Question firstQuestion = questions.get(0);
        assertEquals("Was ist die Hauptstadt von Deutschland?", firstQuestion.getQuestion());
        assertEquals(3, firstQuestion.getAnswers().size());
        assertTrue(firstQuestion.getAnswers().contains("Berlin"));
        assertEquals("Berlin", firstQuestion.getCorrectAnswer());
    }

    @Test
    void testEmptyFile() {
        List<Question> questions = questionReader.readQuestions("test-questions-empty.txt");

        assertTrue(questions.isEmpty(), "No questions should be parsed from an empty file");
    }

    @Test
    void testFileWithComments() {
        List<Question> questions = questionReader.readQuestions("test-questions-with-comments.txt");

        assertFalse(questions.isEmpty(), "Questions should be parsed, ignoring comments");
        assertEquals(1, questions.size(), "Should parse questions while ignoring comment lines");
    }

    @Test
    void testNonExistentFile() {
        List<Question> questions = questionReader.readQuestions("non-existent-file.txt");

        assertTrue(questions.isEmpty(), "Should return empty list for non-existent file");
    }

    @Test
    void testPartialQuestionParsing() {
        List<Question> questions = questionReader.readQuestions("test-questions-partial.txt");

        assertTrue(questions.isEmpty() || questions.size() < 3,
                "Should handle partial or incomplete question sets");
    }

    @Test
    void testMultipleQuestionsParsing() {
        List<Question> questions = questionReader.readQuestions("test-questions-multiple.txt");

        assertTrue(questions.size() >= 2, "Should parse multiple questions");

        // Optional: Check specific details about multiple questions
        for (Question q : questions) {
            assertNotNull(q.getQuestion(), "Question text should not be null");
            assertEquals(3, q.getAnswers().size(), "Each question should have 3 answers");
            assertNotNull(q.getCorrectAnswer(), "Correct answer should be specified");
        }
    }
}