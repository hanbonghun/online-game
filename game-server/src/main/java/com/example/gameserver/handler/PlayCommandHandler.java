
package com.example.gameserver.handler;

import com.example.gameserver.manager.GameRoomManager;
import com.example.gameserver.model.GameChoice;
import com.example.gameserver.model.GameResult;
import com.example.gameserver.model.GameRoom;
import com.example.gameserver.service.GameMessageSender;
import com.example.gameserver.service.GameService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlayCommandHandler implements PayloadCommandHandler<PlayCommandHandler.PlayData> {

    private final GameRoomManager gameRoomManager;
    private final GameService gameService;
    private final GameMessageSender messageSender;

    @Override
    public void handle(WebSocketSession session, PlayData data) {
        GameChoice choice = data.getChoice();
        String gameId = (String) session.getAttributes().get("gameId");

        if (gameId == null) {
            messageSender.sendError(session, "Not in a game");
            return;
        }

        GameRoom gameRoom = gameRoomManager.getGameRoom(gameId);
        if (gameRoom == null) {
            messageSender.sendError(session, "Game not found");
            return;
        }

        processGameChoice(session, gameRoom, choice);
    }

    @Override
    public GameCommand getCommand() {
        return GameCommand.PLAY;
    }

    @Override
    public Class<PlayData> getDataType() {
        return PlayData.class;
    }

    private void processGameChoice(WebSocketSession session, GameRoom gameRoom, GameChoice choice) {
        String playerId = (String) session.getAttributes().get("playerId");
        gameRoomManager.registerPlayerChoice(gameRoom, playerId, choice);

        if (gameRoom.isBothPlayersSelected()) {
            determineWinner(gameRoom);
        }
    }

    private void determineWinner(GameRoom gameRoom) {
        GameResult result = gameService.determineWinner(
            gameRoom.getPlayer1Id(), gameRoom.getPlayer1Choice(),
            gameRoom.getPlayer2Id(), gameRoom.getPlayer2Choice()
        );

        gameRoomManager.handleGameResult(gameRoom, result);
    }

    @Data
    public static class PlayData {

        private GameChoice choice;
    }
}

