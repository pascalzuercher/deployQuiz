package com.example.Quiz.service;

import com.example.Quiz.data.questionHandling.Question;
import com.example.Quiz.model.Game;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
     * Creates a new game with a random ID and returns the ID
     */
    public String createNewGame(List<Question> questions, String gameName) {
        String gameId = UUID.randomUUID().toString();
        Game game = new Game(gameId, questions);
        game.setGameName(gameName);
        games.put(gameId, game);
        return gameId;
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

    /**
     * Returns information about all available games
     */
    public List<Map<String, Object>> getAllGames() {
        return games.values().stream()
                .map(game -> {
                    Map<String, Object> gameInfo = new HashMap<>();
                    gameInfo.put("id", game.getId());
                    gameInfo.put("name", game.getGameName());
                    gameInfo.put("inProgress", game.isInProgress());
                    gameInfo.put("playerCount", game.getPlayers().size());
                    return gameInfo;
                })
                .collect(Collectors.toList());
    }
}