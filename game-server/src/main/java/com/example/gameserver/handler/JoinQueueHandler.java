package com.example.gameserver.handler;

import com.example.gameserver.handler.GameCommand;
import com.example.gameserver.manager.GameRoomManager;
import com.example.gameserver.service.GameMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
@Slf4j
public class JoinQueueHandler implements SimpleCommandHandler {

    @Override
    public void handle(WebSocketSession session) {
        if (gameRoomManager.isPlayerInQueue(session)) {
            messageSender.sendError(session, "Already in queue");
            return;
        }

        if (gameRoomManager.isPlayerInGame(session)) {
            messageSender.sendError(session, "Already in game");
            return;
        }

        gameRoomManager.addToQueue(session);
    }

    private final GameRoomManager gameRoomManager;
    private final GameMessageSender messageSender;

    @Override
    public GameCommand getCommand() {
        return GameCommand.JOIN_QUEUE;
    }

}
