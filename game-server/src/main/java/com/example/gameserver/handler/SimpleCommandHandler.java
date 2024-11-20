package com.example.gameserver.handler;

import com.example.gameserver.common.GameCommandHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

public interface SimpleCommandHandler extends GameCommandHandler {
    void handle(WebSocketSession session);
}
