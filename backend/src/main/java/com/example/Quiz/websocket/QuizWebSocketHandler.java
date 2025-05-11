package com.example.Quiz.websocket;

import com.example.Quiz.data.questionHandling.Question;
import com.example.Quiz.data.questionHandling.QuestionReader;
import com.example.Quiz.model.Game;
import com.example.Quiz.model.Player;
import com.example.Quiz.service.GameManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QuizWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Maps WebSocket sessions to game IDs
    private final Map<WebSocketSession, String> sessionToGameId = new ConcurrentHashMap<>();

    // Maps WebSocket sessions to player IDs
    private final Map<WebSocketSession, String> sessionToPlayerId = new ConcurrentHashMap<>();

    // Track the first correct answer for each question per game
    private final Map<String, String> gameToFirstCorrectPlayerId = new ConcurrentHashMap<>();

    private final GameManager gameManager;
    private final QuestionReader questionReader;

    // Default game ID for backward compatibility
    private static final String DEFAULT_GAME_ID = "default";

    @Autowired
    public QuizWebSocketHandler(GameManager gameManager) {
        this.gameManager = gameManager;
        this.questionReader = new QuestionReader();

        // Create a default game for backward compatibility
        List<Question> questions = questionReader.readQuestions("textFiles/questions_2021.txt");
        gameManager.createGame(DEFAULT_GAME_ID, questions);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Neuer Spieler verbunden: " + session.getId());
        // No longer automatically sending a waiting status
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String action = json.get("action").asText();

        switch (action) {
            case "join" -> handleJoinMessage(session, json);
            case "startGame" -> handleStartGameMessage(session);
            case "answer" -> handleAnswerMessage(session, json);
            case "joinSpecificGame" -> {
                if (json.has("gameId") && json.has("name")) {
                    String gameId = json.get("gameId").asText();
                    String playerName = json.get("name").asText();
                    joinSpecificGame(session, gameId, playerName);
                }
            }
            case "getAvailableGames" -> handleGetAvailableGames(session);
            case "createNewGame" -> {
                if (json.has("name") && json.has("gameName")) {
                    String playerName = json.get("name").asText();
                    String gameName = json.get("gameName").asText();
                    handleCreateNewGame(session, playerName, gameName);
                }
            }
        }
    }

    private void handleGetAvailableGames(WebSocketSession session) throws IOException {
        List<Map<String, Object>> availableGames = gameManager.getAllGames();

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("action", "availableGames");
        responseData.put("games", availableGames);

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(responseData)));
    }

    private void handleCreateNewGame(WebSocketSession session, String playerName, String gameName) throws IOException {
        System.out.println("Creating new game: " + gameName + " for player: " + playerName);

        // Create a new game with the specified name
        List<Question> questions = questionReader.readQuestions("textFiles/questions_2021.txt");
        String gameId = gameManager.createNewGame(questions, gameName);

        // First, send the game created confirmation
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "action", "gameCreated",
                "gameId", gameId,
                "gameName", gameName
        ))));

        // Then, join this new game
        joinSpecificGame(session, gameId, playerName);
    }

    private void handleJoinMessage(WebSocketSession session, JsonNode json) throws IOException {
        String name = json.get("name").asText();

        // For backward compatibility, use the default game
        joinSpecificGame(session, DEFAULT_GAME_ID, name);
    }

    private void joinSpecificGame(WebSocketSession session, String gameId, String playerName) throws IOException {
        System.out.println("Player " + playerName + " is joining game: " + gameId);

        Game game = gameManager.getGame(gameId);

        if (game == null) {
            session.sendMessage(new TextMessage("{\"action\":\"error\",\"message\":\"Game session not found\"}"));
            return;
        }

        // Create player with a unique ID
        Player player = new Player(playerName, session);

        // Associate session with game ID and player ID
        sessionToGameId.put(session, gameId);
        sessionToPlayerId.put(session, player.getId());

        // Add player to game
        game.addPlayer(player);

        System.out.println(playerName + " ist dem Spiel " + gameId + " beigetreten.");

        // Send current game state to the player
        if (game.isInProgress()) {
            session.sendMessage(new TextMessage("{\"action\":\"gameStatus\",\"status\":\"inProgress\"}"));
        } else {
            // Send waiting status with game info
            Map<String, Object> waitingStatus = new HashMap<>();
            waitingStatus.put("action", "gameStatus");
            waitingStatus.put("status", "waiting");
            waitingStatus.put("gameId", gameId);
            waitingStatus.put("gameName", game.getGameName());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(waitingStatus)));

            // Make first player the host
            boolean isHost = game.getPlayers().size() == 1;
            System.out.println("Is player " + playerName + " the host? " + isHost);
            if (isHost) {
                Map<String, Object> hostStatus = new HashMap<>();
                hostStatus.put("action", "hostStatus");
                hostStatus.put("isHost", true);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(hostStatus)));
            }

            // Broadcast updated player list to all in this game
            broadcastPlayerList(game);
        }
    }

    private void handleStartGameMessage(WebSocketSession session) throws IOException {
        String gameId = sessionToGameId.get(session);
        if (gameId == null) {
            return;
        }

        Game game = gameManager.getGame(gameId);
        if (game == null || game.isInProgress()) {
            return;
        }

        game.setInProgress(true);
        game.resetScores();

        // Broadcast game started to all players
        broadcastToGame(game, "{\"action\":\"gameStarted\"}");

        // Start first question
        startNextQuestion(game);
    }

    private void handleAnswerMessage(WebSocketSession session, JsonNode json) throws IOException {
        String gameId = sessionToGameId.get(session);
        String playerId = sessionToPlayerId.get(session);

        if (gameId == null || playerId == null) {
            return;
        }

        Game game = gameManager.getGame(gameId);
        if (game == null || !game.isInProgress()) {
            return;
        }

        Player player = game.getPlayer(playerId);
        if (player == null || player.hasAnswered()) {
            return;
        }

        String answer = json.get("answer").asText();
        Question currentQuestion = game.getCurrentQuestion();

        if (currentQuestion == null) {
            return;
        }

        // Check if answer is correct
        boolean isCorrect = answer.equals(currentQuestion.getCorrectAnswer());

        // If correct and this is the first correct answer for this question
        String questionKey = gameId + "-" + game.getCurrentQuestionIndex();
        if (isCorrect && !gameToFirstCorrectPlayerId.containsKey(questionKey)) {
            // This player is the first with the correct answer!
            gameToFirstCorrectPlayerId.put(questionKey, playerId);
            player.addPoints(1);

            // Send a special notification to this player
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "action", "answerResult",
                    "correct", true,
                    "fastest", true,
                    "score", player.getScore()
            ))));
        } else {
            // Either incorrect or not the first correct answer
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "action", "answerResult",
                    "correct", isCorrect,
                    "fastest", false,
                    "score", player.getScore()
            ))));
        }

        // Mark player as answered
        player.setHasAnswered(true);

        // Broadcast updated scores
        broadcastScores(game);

        // Check if all players answered
        if (game.haveAllPlayersAnswered()) {
            // Cancel timer
            game.cancelTimer();

            // Process end of question
            processEndOfQuestion(game);
        }
    }

    private void startNextQuestion(Game game) {
        if (game == null) {
            return;
        }

        // Check if we're at the end of questions
        if (game.isLastQuestion() || game.getCurrentQuestion() == null) {
            endGame(game);
            return;
        }

        try {
            Question currentQuestion = game.getCurrentQuestion();

            // Prepare question data without the correct answer
            Map<String, Object> questionData = new HashMap<>();
            questionData.put("action", "question");
            questionData.put("questionNumber", game.getCurrentQuestionIndex() + 1);
            questionData.put("totalQuestions", game.getQuestions().size());
            questionData.put("question", currentQuestion.getQuestion());
            questionData.put("answers", currentQuestion.getAnswers());
            questionData.put("timeLimit", 30); // Fixed time limit for now

            // Broadcast question to all players
            broadcastToGame(game, objectMapper.writeValueAsString(questionData));

            // Start timer for this question
            startQuestionTimer(game, 30);

        } catch (Exception e) {
            System.err.println("Fehler beim Senden der Frage: " + e.getMessage());
        }
    }

    private void startQuestionTimer(Game game, int seconds) {
        // Cancel existing timer if any
        game.cancelTimer();

        // Create new timer
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    // Time's up for this question
                    processEndOfQuestion(game);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, seconds * 1000);

        // Store timer reference
        game.setQuestionTimer(timer);
    }

    private void processEndOfQuestion(Game game) throws IOException {
        if (game == null) {
            return;
        }

        Question currentQuestion = game.getCurrentQuestion();
        if (currentQuestion == null) {
            return;
        }

        // Get the first correct player for this question (if any)
        String questionKey = game.getId() + "-" + game.getCurrentQuestionIndex();
        String fastestPlayerId = gameToFirstCorrectPlayerId.get(questionKey);

        // Create result data
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("action", "revealAnswer");
        resultData.put("correctAnswer", currentQuestion.getCorrectAnswer());
        resultData.put("questionNumber", game.getCurrentQuestionIndex() + 1);

        // Add fastest player info if someone answered correctly
        if (fastestPlayerId != null) {
            Player fastestPlayer = game.getPlayer(fastestPlayerId);
            if (fastestPlayer != null) {
                resultData.put("fastestPlayer", fastestPlayer.getDisplayName());
            }
        }

        // Reveal correct answer to all players
        broadcastToGame(game, objectMapper.writeValueAsString(resultData));

        // Schedule next question after delay
        Timer delayTimer = new Timer();
        delayTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    game.nextQuestion();
                    startNextQuestion(game);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 5000); // 5 seconds delay
    }

    private void endGame(Game game) {
        if (game == null) {
            return;
        }

        game.setInProgress(false);

        try {
            // Prepare results
            Map<String, Object> results = new HashMap<>();
            results.put("action", "gameOver");
            results.put("scores", game.getPlayerScores());

            Map.Entry<String, Integer> winner = game.getWinner();
            if (winner != null) {
                results.put("winner", winner.getKey());
                results.put("winnerScore", winner.getValue());
            }

            // Broadcast results
            broadcastToGame(game, objectMapper.writeValueAsString(results));

            // Clean up tracking map - remove all entries for this game
            String gameId = game.getId();
            gameToFirstCorrectPlayerId.entrySet().removeIf(entry ->
                    entry.getKey().startsWith(gameId + "-"));

        } catch (Exception e) {
            System.err.println("Fehler beim Beenden des Spiels: " + e.getMessage());
        }
    }

    private void broadcastPlayerList(Game game) throws IOException {
        if (game == null) {
            return;
        }

        Map<String, Object> playerList = new HashMap<>();
        playerList.put("action", "playerList");
        playerList.put("players", game.getPlayerNames());

        broadcastToGame(game, objectMapper.writeValueAsString(playerList));
    }

    private void broadcastScores(Game game) throws IOException {
        if (game == null) {
            return;
        }

        broadcastToGame(game, objectMapper.writeValueAsString(Map.of(
                "action", "scoreUpdate",
                "scores", game.getPlayerScores()
        )));
    }

    private void broadcastToGame(Game game, String message) throws IOException {
        if (game == null) {
            return;
        }

        TextMessage textMessage = new TextMessage(message);
        for (Player player : game.getPlayers().values()) {
            WebSocketSession session = player.getSession();
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // Get game ID and player ID before removing
        String gameId = sessionToGameId.remove(session);
        String playerId = sessionToPlayerId.remove(session);

        if (gameId != null && playerId != null) {
            Game game = gameManager.getGame(gameId);
            if (game != null) {
                // Get player and remove from game
                Player player = game.getPlayer(playerId);
                if (player != null) {
                    String playerName = player.getName();
                    game.removePlayer(playerId);
                    System.out.println("Spieler getrennt: " + playerName + " aus Spiel " + gameId);

                    try {
                        // Broadcast updated player list
                        broadcastPlayerList(game);

                        // End game if no players left
                        if (game.getPlayers().isEmpty() && game.isInProgress()) {
                            game.setInProgress(false);
                        }
                    } catch (IOException e) {
                        System.err.println("Fehler beim Aktualisieren nach Spieler-Disconnect: " + e.getMessage());
                    }
                }

                // If no more players and not default game, remove the game
                if (game.getPlayers().isEmpty() && !DEFAULT_GAME_ID.equals(gameId)) {
                    gameManager.removeGame(gameId);
                }
            }
        }
    }
}