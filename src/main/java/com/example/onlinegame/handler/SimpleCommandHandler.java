package com.example.onlinegame.handler;

import com.example.onlinegame.common.GameCommandHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

public interface SimpleCommandHandler extends GameCommandHandler {
    void handle(WebSocketSession session);
}
