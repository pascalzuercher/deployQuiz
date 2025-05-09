package com.example.Quiz.service;

import com.example.Quiz.data.questionHandling.Question;
import com.example.Quiz.model.Game;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages quiz games
 */
@Service
public class GameManager {
    private final Map<String, Game> games = new ConcurrentHashMap<>();


    /**
     * Creates a new game with the specified ID
     */
    public void createGame(String gameId, List<Question> questions) {
        Game game = new Game(gameId, questions);
        games.put(gameId, game);
    }

    /**
     * Gets a game by ID
     */
    public Game getGame(String gameId) {
        return games.get(gameId);
    }

    /**
     * Removes a game
     */
    public void removeGame(String gameId) {
        Game game = games.get(gameId);
        if (game != null) {
            game.cancelTimer();
            games.remove(gameId);
        }
    }
}