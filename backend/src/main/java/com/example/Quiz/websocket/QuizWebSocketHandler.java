package com.example.Quiz.websocket;

import com.example.Quiz.data.questionHandling.Question;
import com.example.Quiz.data.questionHandling.QuestionReader;
import com.example.Quiz.service.GameSessionManager;
import com.example.Quiz.service.GameSessionManager.GameSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class QuizWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Maps WebSocket sessions to game session IDs
    private final ConcurrentMap<WebSocketSession, String> sessionToGameId = new ConcurrentHashMap<>();

    // Maps WebSocket sessions to player names
    private final ConcurrentMap<WebSocketSession, String> sessionToPlayerName = new ConcurrentHashMap<>();

    // Maps game IDs to sets of WebSocket sessions for broadcasting
    private final ConcurrentMap<String, Set<WebSocketSession>> gameIdToSessions = new ConcurrentHashMap<>();

    // Timers for each game session
    private final ConcurrentMap<String, Timer> gameTimers = new ConcurrentHashMap<>();

    private final GameSessionManager gameSessionManager;
    private final QuestionReader questionReader;

    // Default game ID for backward compatibility
    private static final String DEFAULT_GAME_ID = "default";

    @Autowired
    public QuizWebSocketHandler(GameSessionManager gameSessionManager) {
        this.gameSessionManager = gameSessionManager;
        this.questionReader = new QuestionReader();

        // Create a default game session for backward compatibility
        List<Question> questions = questionReader.readQuestions("textFiles/questions_2021.txt");
        gameSessionManager.createGameSession(DEFAULT_GAME_ID, questions);

        System.out.println("📘 Quiz WebSocket Handler initialized");
        System.out.println("Trete der Lobby unter: http://localhost:8080 bei. Viel Spass!");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Neuer Spieler verbunden: " + session.getId());

        // Default game status message
        try {
            session.sendMessage(new TextMessage("{\"action\":\"gameStatus\",\"status\":\"waiting\"}"));
        } catch (IOException e) {
            System.err.println("Fehler beim Senden des Spielstatus: " + e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String action = json.get("action").asText();

        switch (action) {
            case "join" -> {
                handleJoinMessage(session, json);
            }

            case "startGame" -> {
                handleStartGameMessage(session);
            }

            case "answer" -> {
                handleAnswerMessage(session, json);
            }

            case "joinSpecificGame" -> {
                if (json.has("gameId")) {
                    String gameId = json.get("gameId").asText();
                    String playerName = json.get("name").asText();
                    joinSpecificGame(session, gameId, playerName);
                }
            }
        }
    }

    private void handleJoinMessage(WebSocketSession session, JsonNode json) throws IOException {
        String name = json.get("name").asText();

        // For backward compatibility, use the default game
        joinSpecificGame(session, DEFAULT_GAME_ID, name);
    }

    private void joinSpecificGame(WebSocketSession session, String gameId, String playerName) throws IOException {
        GameSession gameSession = gameSessionManager.getSession(gameId);

        if (gameSession == null) {
            session.sendMessage(new TextMessage("{\"action\":\"error\",\"message\":\"Game session not found\"}"));
            return;
        }

        // Associate session with game ID and player name
        sessionToGameId.put(session, gameId);
        sessionToPlayerName.put(session, playerName);

        // Add session to game's session set
        gameIdToSessions.computeIfAbsent(gameId, k -> Collections.synchronizedSet(new HashSet<>()))
                .add(session);

        // Add player to game session
        gameSession.addPlayer(playerName);

        System.out.println(playerName + " ist dem Spiel " + gameId + " beigetreten.");

        // Send current game state to the player
        if (gameSession.isGameInProgress()) {
            session.sendMessage(new TextMessage("{\"action\":\"gameStatus\",\"status\":\"inProgress\"}"));
        } else {
            // Make first player the host
            boolean isHost = gameSession.getPlayerScores().size() == 1;
            if (isHost) {
                session.sendMessage(new TextMessage("{\"action\":\"hostStatus\",\"isHost\":true}"));
            }

            // Broadcast updated player list to all in this game
            broadcastPlayerList(gameId);
        }
    }

    private void handleStartGameMessage(WebSocketSession session) throws IOException {
        String gameId = sessionToGameId.get(session);
        if (gameId == null) {
            return;
        }

        GameSession gameSession = gameSessionManager.getSession(gameId);
        if (gameSession == null || gameSession.isGameInProgress()) {
            return;
        }

        gameSession.setGameInProgress(true);
        gameSession.setCurrentQuestionIndex(0);

        // Clear scores
        for (String player : gameSession.getPlayerScores().keySet()) {
            gameSession.getPlayerScores().put(player, 0);
        }

        // Broadcast game started to all players in this game
        broadcastToGame(gameId, "{\"action\":\"gameStarted\"}");

        // Start first question
        startNextQuestion(gameId);
    }

    private void handleAnswerMessage(WebSocketSession session, JsonNode json) throws IOException {
        String gameId = sessionToGameId.get(session);
        String playerName = sessionToPlayerName.get(session);

        if (gameId == null || playerName == null) {
            return;
        }

        GameSession gameSession = gameSessionManager.getSession(gameId);
        if (gameSession == null || !gameSession.isGameInProgress()) {
            return;
        }

        if (gameSession.hasPlayerAnswered(playerName)) {
            // Player already answered
            return;
        }

        String answer = json.get("answer").asText();
        Question currentQuestion = gameSession.getCurrentQuestion();

        if (currentQuestion == null) {
            return;
        }

        // Check if answer is correct
        boolean isCorrect = answer.equals(currentQuestion.getCorrectAnswer());
        if (isCorrect) {
            gameSession.addScore(playerName, 1);
        }

        // Mark player as answered
        gameSession.markPlayerAnswered(playerName);

        // Send answer result to player
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "action", "answerResult",
                "correct", isCorrect,
                "score", gameSession.getScore(playerName)
        ))));

        // Broadcast updated scores
        broadcastScores(gameId);

        // Check if all players answered
        if (gameSession.haveAllPlayersAnswered()) {
            // Cancel timer
            Timer timer = gameTimers.get(gameId);
            if (timer != null) {
                timer.cancel();
                gameTimers.remove(gameId);
            }

            // Process end of question
            processEndOfQuestion(gameId);
        }
    }

    private void startNextQuestion(String gameId) {
        GameSession gameSession = gameSessionManager.getSession(gameId);
        if (gameSession == null) {
            return;
        }

        // Clear previous answers
        gameSession.clearAnsweredPlayers();

        // Check if we're at the end of questions
        if (gameSession.isLastQuestion() || gameSession.getCurrentQuestion() == null) {
            endGame(gameId);
            return;
        }

        try {
            Question currentQuestion = gameSession.getCurrentQuestion();

            // Prepare question data without the correct answer
            Map<String, Object> questionData = new HashMap<>();
            questionData.put("action", "question");
            questionData.put("questionNumber", gameSession.getCurrentQuestionIndex() + 1);
            questionData.put("totalQuestions", gameSession.getQuestions().size());
            questionData.put("question", currentQuestion.getQuestion());
            questionData.put("answers", currentQuestion.getAnswers());
            questionData.put("timeLimit", 30); // Fixed time limit for now

            // Broadcast question to all players in this game
            broadcastToGame(gameId, objectMapper.writeValueAsString(questionData));

            // Start timer for this question
            startQuestionTimer(gameId, 30);

        } catch (Exception e) {
            System.err.println("Fehler beim Senden der Frage: " + e.getMessage());
        }
    }

    private void startQuestionTimer(String gameId, int seconds) {
        // Cancel existing timer if any
        Timer existingTimer = gameTimers.get(gameId);
        if (existingTimer != null) {
            existingTimer.cancel();
        }

        // Create new timer
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    // Time's up for this question
                    processEndOfQuestion(gameId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, seconds * 1000);

        // Store timer reference
        gameTimers.put(gameId, timer);
    }

    private void processEndOfQuestion(String gameId) throws IOException {
        GameSession gameSession = gameSessionManager.getSession(gameId);
        if (gameSession == null) {
            return;
        }

        Question currentQuestion = gameSession.getCurrentQuestion();
        if (currentQuestion == null) {
            return;
        }

        // Reveal correct answer to all players
        broadcastToGame(gameId, objectMapper.writeValueAsString(Map.of(
                "action", "revealAnswer",
                "correctAnswer", currentQuestion.getCorrectAnswer(),
                "questionNumber", gameSession.getCurrentQuestionIndex() + 1
        )));

        // Schedule next question after delay
        Timer delayTimer = new Timer();
        delayTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    gameSession.nextQuestion();
                    startNextQuestion(gameId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 5000); // 5 seconds delay
    }

    private void endGame(String gameId) {
        GameSession gameSession = gameSessionManager.getSession(gameId);
        if (gameSession == null) {
            return;
        }

        gameSession.setGameInProgress(false);

        try {
            // Prepare results
            Map<String, Object> results = new HashMap<>();
            results.put("action", "gameOver");
            results.put("scores", gameSession.getPlayerScores());

            Map.Entry<String, Integer> winner = gameSession.getWinner();
            if (winner != null) {
                results.put("winner", winner.getKey());
                results.put("winnerScore", winner.getValue());
            }

            // Broadcast results
            broadcastToGame(gameId, objectMapper.writeValueAsString(results));

        } catch (Exception e) {
            System.err.println("Fehler beim Beenden des Spiels: " + e.getMessage());
        }
    }

    private void broadcastPlayerList(String gameId) throws IOException {
        GameSession gameSession = gameSessionManager.getSession(gameId);
        if (gameSession == null) {
            return;
        }

        Map<String, Object> playerList = new HashMap<>();
        playerList.put("action", "playerList");
        playerList.put("players", new ArrayList<>(gameSession.getPlayerScores().keySet()));

        broadcastToGame(gameId, objectMapper.writeValueAsString(playerList));
    }

    private void broadcastScores(String gameId) throws IOException {
        GameSession gameSession = gameSessionManager.getSession(gameId);
        if (gameSession == null) {
            return;
        }

        broadcastToGame(gameId, objectMapper.writeValueAsString(Map.of(
                "action", "scoreUpdate",
                "scores", gameSession.getPlayerScores()
        )));
    }

    private void broadcastToGame(String gameId, String message) throws IOException {
        Set<WebSocketSession> sessions = gameIdToSessions.get(gameId);
        if (sessions == null) {
            return;
        }

        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // Get game ID and player name before removing
        String gameId = sessionToGameId.remove(session);
        String playerName = sessionToPlayerName.remove(session);

        if (gameId != null && playerName != null) {
            // Remove session from game sessions
            Set<WebSocketSession> gameSessions = gameIdToSessions.get(gameId);
            if (gameSessions != null) {
                gameSessions.remove(session);

                // If no more sessions, clean up
                if (gameSessions.isEmpty()) {
                    gameIdToSessions.remove(gameId);

                    // Cancel any timers
                    Timer timer = gameTimers.remove(gameId);
                    if (timer != null) {
                        timer.cancel();
                    }

                    // Only remove non-default game sessions
                    if (!DEFAULT_GAME_ID.equals(gameId)) {
                        gameSessionManager.removeSession(gameId);
                    }
                }
            }

            // Remove player from game session
            GameSession gameSession = gameSessionManager.getSession(gameId);
            if (gameSession != null) {
                gameSession.removePlayer(playerName);

                try {
                    // Broadcast updated player list
                    broadcastPlayerList(gameId);

                    // End game if no players left
                    if (gameSession.getPlayerScores().isEmpty() && gameSession.isGameInProgress()) {
                        gameSession.setGameInProgress(false);
                    }
                } catch (IOException e) {
                    System.err.println("Fehler beim Aktualisieren nach Spieler-Disconnect: " + e.getMessage());
                }
            }

            System.out.println("Spieler getrennt: " + playerName + " aus Spiel " + gameId);
        }
    }
}