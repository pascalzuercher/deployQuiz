package com.example.Quiz.model;

import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

/**
 * Represents a player in the quiz game
 */
public class Player {
    private final String id;
    private final String name;
    private final WebSocketSession session;
    private int score;
    private boolean hasAnswered;
    private String displayName; // Added to handle duplicate names

    public Player(String name, WebSocketSession session) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.session = session;
        this.score = 0;
        this.hasAnswered = false;
        this.displayName = name; // Initially same as name
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void addPoints(int points) {
        this.score += points;
    }

    public boolean hasAnswered() {
        return hasAnswered;
    }

    public void setHasAnswered(boolean hasAnswered) {
        this.hasAnswered = hasAnswered;
    }

    public void resetAnswer() {
        this.hasAnswered = false;
    }

    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}