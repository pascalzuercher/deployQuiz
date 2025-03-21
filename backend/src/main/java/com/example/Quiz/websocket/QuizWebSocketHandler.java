package com.example.Quiz.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;

@Component
public class QuizWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<WebSocketSession, String> players = new HashMap<>();
    private final Map<String, Integer> scores = new HashMap<>();

    // Beispiel-Frage
    private final String currentQuestion = "Was ist 2 + 2?";
    private final List<String> currentAnswers = List.of("3", "4", "5");
    private final String correctAnswer = "4";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Neuer Spieler verbunden: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String action = json.get("action").asText();

        switch (action) {
            case "join" -> {
                String name = json.get("name").asText();
                players.put(session, name);
                scores.put(name, 0);
                System.out.println(name + " ist dem Spiel beigetreten.");
                sendQuestionToAll();
            }

            case "answer" -> {
                String name = json.get("name").asText();
                String answer = json.get("answer").asText();

                if (answer.equals(correctAnswer)) {
                    int newScore = scores.getOrDefault(name, 0) + 1;
                    scores.put(name, newScore);
                    session.sendMessage(new TextMessage("{\"action\":\"score\",\"score\":" + newScore + "}"));
                } else {
                    session.sendMessage(new TextMessage("{\"action\":\"score\",\"score\":" + scores.getOrDefault(name, 0) + "}"));
                }
            }
        }
    }

    private void sendQuestionToAll() throws Exception {
        String questionJson = objectMapper.writeValueAsString(Map.of(
                "action", "question",
                "question", currentQuestion,
                "answers", currentAnswers
        ));

        for (WebSocketSession session : players.keySet()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(questionJson));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String name = players.remove(session);
        scores.remove(name);
        System.out.println("Spieler getrennt: " + name);
    }
}
