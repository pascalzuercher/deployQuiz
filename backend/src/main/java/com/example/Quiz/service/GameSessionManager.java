package com.example.Quiz.service;

import com.example.Quiz.data.questionHandling.Question;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages quiz game sessions when supporting multiple concurrent games
 */
@Service
public class GameSessionManager {

    // Map of gameId -> session data
    private final Map<String, GameSession> gameSessions = new ConcurrentHashMap<>();

    /**
     * Creates a new game session and returns the session ID
     */
    public String createGameSession(List<Question> questions) {
        String gameId = UUID.randomUUID().toString();
        createGameSession(gameId, questions);
        return gameId;
    }

    /**
     * Creates a new game session with the specified ID
     */
    public void createGameSession(String gameId, List<Question> questions) {
        GameSession session = new GameSession(gameId, questions);
        gameSessions.put(gameId, session);
    }

    /**
     * Gets a game session by ID
     */
    public GameSession getSession(String gameId) {
        return gameSessions.get(gameId);
    }

    /**
     * Removes a game session
     */
    public void removeSession(String gameId) {
        gameSessions.remove(gameId);
    }

    /**
     * Represents a single quiz game session
     */
    public static class GameSession {
        private final String id;
        private final List<Question> questions;
        private int currentQuestionIndex = 0;
        private boolean gameInProgress = false;
        private final Map<String, Integer> playerScores = new ConcurrentHashMap<>();
        private final Set<String> answeredPlayers = Collections.synchronizedSet(new HashSet<>());

        public GameSession(String id, List<Question> questions) {
            this.id = id;
            this.questions = questions;
        }

        // Getters and setters

        public String getId() {
            return id;
        }

        public List<Question> getQuestions() {
            return questions;
        }

        public int getCurrentQuestionIndex() {
            return currentQuestionIndex;
        }

        public void setCurrentQuestionIndex(int index) {
            this.currentQuestionIndex = index;
        }

        public boolean isGameInProgress() {
            return gameInProgress;
        }

        public void setGameInProgress(boolean inProgress) {
            this.gameInProgress = inProgress;
        }

        public Map<String, Integer> getPlayerScores() {
            return playerScores;
        }

        public void addPlayer(String playerName) {
            playerScores.put(playerName, 0);
        }

        public void removePlayer(String playerName) {
            playerScores.remove(playerName);
            answeredPlayers.remove(playerName);
        }

        public void addScore(String playerName, int points) {
            playerScores.put(playerName, playerScores.getOrDefault(playerName, 0) + points);
        }

        public int getScore(String playerName) {
            return playerScores.getOrDefault(playerName, 0);
        }

        public void markPlayerAnswered(String playerName) {
            answeredPlayers.add(playerName);
        }

        public boolean hasPlayerAnswered(String playerName) {
            return answeredPlayers.contains(playerName);
        }

        public void clearAnsweredPlayers() {
            answeredPlayers.clear();
        }

        public boolean haveAllPlayersAnswered() {
            return answeredPlayers.size() >= playerScores.size() && !playerScores.isEmpty();
        }

        public Question getCurrentQuestion() {
            if (currentQuestionIndex < questions.size()) {
                return questions.get(currentQuestionIndex);
            }
            return null;
        }

        public boolean isLastQuestion() {
            return currentQuestionIndex >= questions.size() - 1;
        }

        public void nextQuestion() {
            currentQuestionIndex++;
            clearAnsweredPlayers();
        }

        public Map<String, Integer> getResults() {
            return playerScores;
        }

        public Map.Entry<String, Integer> getWinner() {
            Optional<Map.Entry<String, Integer>> winner = playerScores.entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue());
            return winner.orElse(null);
        }
    }
}