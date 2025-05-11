package com.example.Quiz.model;

import com.example.Quiz.data.questionHandling.Question;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Represents a single quiz game
 */
public class Game {
    private final String id;
    private String gameName;
    private final List<Question> questions;
    private int currentQuestionIndex;
    private boolean inProgress;
    private final Map<String, Player> players; // Map of player ID -> Player
    private Timer questionTimer;

    public Game(String id, List<Question> questions) {
        this.id = id;
        this.gameName = "Game " + id.substring(0, 7); // Default name is "Game" + first 7 chars of ID
        this.questions = questions;
        this.currentQuestionIndex = 0;
        this.inProgress = false;
        this.players = new ConcurrentHashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public Question getCurrentQuestion() {
        if (currentQuestionIndex < questions.size()) {
            return questions.get(currentQuestionIndex);
        }
        return null;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
    }

    public Map<String, Player> getPlayers() {
        return players;
    }

    public void addPlayer(Player player) {
        // Check for duplicate names and update display name if needed
        updateDisplayNameForDuplicates(player);

        // Add player to the game
        players.put(player.getId(), player);
    }

    /**
     * Updates a player's display name if there are other players with the same name
     * Adds a suffix like "(2)" for duplicates
     */
    private void updateDisplayNameForDuplicates(Player newPlayer) {
        String baseName = newPlayer.getName();

        // Count players with the same base name
        long sameNameCount = players.values().stream()
                .filter(p -> p.getName().equals(baseName))
                .count();

        // If there are other players with the same name,
        // set a new display name with a suffix
        if (sameNameCount > 0) {
            newPlayer.setDisplayName(baseName + " (" + (sameNameCount + 1) + ")");

            // If this is the second player with this name,
            // update the first player's display name too (if needed)
            if (sameNameCount == 1) {
                players.values().stream()
                        .filter(p -> p.getName().equals(baseName) && p.getDisplayName().equals(baseName))
                        .findFirst()
                        .ifPresent(p -> p.setDisplayName(baseName + " (1)"));
            }
        }
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    public Player getPlayerBySession(WebSocketSession session) {
        return players.values().stream()
                .filter(p -> p.getSession().equals(session))
                .findFirst()
                .orElse(null);
    }

    public boolean isLastQuestion() {
        return currentQuestionIndex >= questions.size() - 1;
    }

    public void nextQuestion() {
        currentQuestionIndex++;
        resetPlayerAnswers();
    }

    public void resetPlayerAnswers() {
        players.values().forEach(Player::resetAnswer);
    }

    public boolean haveAllPlayersAnswered() {
        return !players.isEmpty() &&
                players.values().stream().allMatch(Player::hasAnswered);
    }

    public Map<String, Integer> getPlayerScores() {
        // Use displayName instead of name to ensure uniqueness in the score map
        return players.values().stream()
                .collect(Collectors.toMap(
                        Player::getDisplayName,
                        Player::getScore
                ));
    }

    public List<String> getPlayerNames() {
        // Use displayName to show potentially modified names with suffixes
        return players.values().stream()
                .map(Player::getDisplayName)
                .collect(Collectors.toList());
    }

    public void resetScores() {
        players.values().forEach(p -> p.setScore(0));
    }

    public Map.Entry<String, Integer> getWinner() {
        Optional<Player> winner = players.values().stream()
                .max(Comparator.comparingInt(Player::getScore));

        return winner.map(p ->
                        new AbstractMap.SimpleEntry<>(p.getDisplayName(), p.getScore()))
                .orElse(null);
    }

    public Timer getQuestionTimer() {
        return questionTimer;
    }

    public void setQuestionTimer(Timer questionTimer) {
        this.questionTimer = questionTimer;
    }

    public void cancelTimer() {
        if (questionTimer != null) {
            questionTimer.cancel();
            questionTimer = null;
        }
    }
}