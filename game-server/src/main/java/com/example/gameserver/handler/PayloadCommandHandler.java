
package com.example.gameserver.handler;

import com.example.gameserver.common.GameCommandHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

public interface PayloadCommandHandler<T> extends GameCommandHandler {
    Class<T> getDataType();
    void handle(WebSocketSession session, T data);
}