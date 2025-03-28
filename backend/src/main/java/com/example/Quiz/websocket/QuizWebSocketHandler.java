package com.example.Quiz.websocket;

import com.example.Quiz.data.questionHandling.Question;
import com.example.Quiz.data.questionHandling.QuestionReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;

@Component
public class QuizWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<WebSocketSession, String> players = new HashMap<>();
    private final Map<String, Integer> scores = new HashMap<>();
    private final Set<WebSocketSession> answeredPlayers = new HashSet<>();


    private final List<Question> questions;
    private int currentQuestionIndex = 0;

    public QuizWebSocketHandler() {
        QuestionReader reader = new QuestionReader();
        this.questions = reader.readQuestions("textFiles/questions_2021.txt");

        System.out.println("📘 Geladene Fragen: " + questions.size());

        System.out.println("Trete der Lobby unter: http://localhost:8080 bei. Viel Spass!");
    }


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

                Question currentQ = questions.get(currentQuestionIndex);

                if (answer.equals(currentQ.getCorrectAnswer())) {
                    int newScore = scores.getOrDefault(name, 0) + 1;
                    scores.put(name, newScore);
                    session.sendMessage(new TextMessage("{\"action\":\"score\",\"score\":" + newScore + "}"));
                } else {
                    session.sendMessage(new TextMessage("{\"action\":\"score\",\"score\":" + scores.getOrDefault(name, 0) + "}"));
                }

                // nächste Frage nach Antwort senden (kann später verbessert werden mit Synchronisation)
                currentQuestionIndex++;
                sendQuestionToAll();

                answeredPlayers.add(session);

                if (answeredPlayers.containsAll(players.keySet())) {
                    broadcastAllAnswered(10); // z. B. 3 Sekunden Pause
                    answeredPlayers.clear();

                    // Nächste Frage nach Delay
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            currentQuestionIndex++;
                            try {
                                sendQuestionToAll();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, 12000);
                }
            }
        }
    }

    private void broadcastAllAnswered(int delayInSeconds) throws IOException {
        String msg = objectMapper.writeValueAsString(Map.of(
                "action", "allAnswered",
                "delay", delayInSeconds
        ));

        for (WebSocketSession session : players.keySet()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(msg));
            }
        }
    }

    private void sendQuestionToAll() throws Exception {
        if (currentQuestionIndex >= questions.size()) {
            for (WebSocketSession session : players.keySet()) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("{\"action\":\"gameOver\"}"));
                }
            }
            return;
        }

        Question q = questions.get(currentQuestionIndex);

        String questionJson = objectMapper.writeValueAsString(Map.of(
                "action", "question",
                "question", q.getQuestion(),
                "answers", q.getAnswers(),
                "correctAnswer", q.getCorrectAnswer()
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
