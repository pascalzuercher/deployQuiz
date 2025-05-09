package com.example.Quiz.controller;

import com.example.Quiz.data.questionHandling.Question;
import com.example.Quiz.data.questionHandling.QuestionReader;
import com.example.Quiz.service.GameSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuestionReader questionReader;
    private final GameSessionManager gameSessionManager;

    @Autowired
    public QuizController(GameSessionManager gameSessionManager) {
        this.gameSessionManager = gameSessionManager;
        this.questionReader = new QuestionReader();
    }

    /**
     * Creates a new quiz game session
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createGame(@RequestParam(required = false) String questionFile) {
        // Default to questions_2021.txt if not specified
        String fileToUse = questionFile != null ? questionFile : "textFiles/questions_2021.txt";

        List<Question> questions = questionReader.readQuestions(fileToUse);
        if (questions.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No questions found in the specified file"
            ));
        }

        String gameId = gameSessionManager.createGameSession(questions);
        return ResponseEntity.ok(Map.of(
                "gameId", gameId,
                "questionCount", String.valueOf(questions.size())
        ));
    }

    /**
     * Gets information about an existing game
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<?> getGameInfo(@PathVariable String gameId) {
        GameSessionManager.GameSession session = gameSessionManager.getSession(gameId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "gameId", session.getId(),
                "inProgress", session.isGameInProgress(),
                "questionCount", session.getQuestions().size(),
                "currentQuestionIndex", session.getCurrentQuestionIndex(),
                "playerCount", session.getPlayerScores().size()
        ));
    }
}
