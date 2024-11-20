package com.example.gameserver.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

@Component
@Slf4j
public class SessionManager {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void registerSession(String playerId, WebSocketSession session) {
        sessions.put(playerId, session);
    }

    public void removeSession(String playerId) {
        sessions.remove(playerId);
    }

    public WebSocketSession getSession(String playerId) {
        return sessions.get(playerId);
    }
}