package com.example.onlinegame.service;

import com.example.onlinegame.handler.ResponseType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameMessageSender {
    private final ObjectMapper objectMapper;

    public void sendMessage(WebSocketSession session, ResponseType type, String message) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", type);
            response.put("message", message);

            String json = objectMapper.writeValueAsString(response);
            session.send(Mono.just(session.textMessage(json))).subscribe();
        } catch (JsonProcessingException e) {
            log.error("Failed to send message", e);
        }
    }

    public void sendError(WebSocketSession session, String errorMessage) {
        sendMessage(session, ResponseType.ERROR, errorMessage);
    }

    public <T> void sendGameState(WebSocketSession session, ResponseType type, String message, T data) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", type);
            response.put("message", message);
            response.put("data", data);

            String json = objectMapper.writeValueAsString(response);
            session.send(Mono.just(session.textMessage(json))).subscribe();
        } catch (JsonProcessingException e) {
            log.error("Failed to send message", e);
        }
    }
}