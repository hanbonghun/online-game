
package com.example.onlinegame.handler;

import com.example.onlinegame.common.GameCommandHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

public interface PayloadCommandHandler<T> extends GameCommandHandler {
    Class<T> getDataType();
    void handle(WebSocketSession session, T data);
}