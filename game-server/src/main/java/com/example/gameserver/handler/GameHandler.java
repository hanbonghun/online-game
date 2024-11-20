package com.example.gameserver.handler;

import com.example.gameserver.common.GameCommandDispatcher;
import com.example.gameserver.manager.GameRoomManager;
import com.example.gameserver.manager.SessionManager;
import com.example.gameserver.service.GameMessageSender;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameHandler implements WebSocketHandler {

    private final GameCommandDispatcher commandDispatcher;
    private final GameMessageSender messageSender;
    private final SessionManager sessionManager;
    private final GameRoomManager gameRoomManager;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        log.info("New WebSocket connection attempt");

        return session.receive()
            .doOnSubscribe(sub -> {
                String playerId = UUID.randomUUID().toString();
                session.getAttributes().put("playerId", playerId);
                sessionManager.registerSession(playerId, session);  // 세션 등록
                messageSender.sendMessage(session, ResponseType.CONNECTED,
                    "Connected successfully. Your ID: " + playerId);
            })
            .doOnNext(message -> handleMessage(session, message))
            .doOnError(error -> {
                log.error("WebSocket error: ", error);
                messageSender.sendError(session, "Internal server error");
            })
            .doFinally(signalType -> {
                log.info("Connection closing: {}", signalType);
                handleDisconnect(session);
            })
            .then();
    }

    private void handleMessage(WebSocketSession session, WebSocketMessage message) {
        if (message.getType() != WebSocketMessage.Type.TEXT) {
            messageSender.sendError(session, "Only text messages are supported");
            return;
        }

        try {
            String payload = message.getPayloadAsText();
            log.debug("Received message: {}", payload);

            commandDispatcher.dispatch(session, payload);
        } catch (Exception e) {
            log.error("Error processing message", e);
            messageSender.sendError(session, e.getMessage());
        }
    }

    private void handleDisconnect(WebSocketSession session) {
        String playerId = (String) session.getAttributes().get("playerId");
        if (playerId != null) {
            sessionManager.removeSession(playerId);
            gameRoomManager.handlePlayerDisconnect(playerId);
        }
    }
}
