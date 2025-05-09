package com.example.Quiz.config;

import com.example.Quiz.websocket.QuizWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final QuizWebSocketHandler handler;

    public WebSocketConfig(QuizWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/quiz").setAllowedOrigins("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // Set the maximum message size (1MB)
        container.setMaxTextMessageBufferSize(1024 * 1024);
        container.setMaxBinaryMessageBufferSize(1024 * 1024);
        // Set session timeout (in seconds)
        container.setMaxSessionIdleTimeout(3600000L);
        return container;
    }
}