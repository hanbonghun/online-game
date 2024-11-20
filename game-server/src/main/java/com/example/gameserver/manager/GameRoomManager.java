package com.example.gameserver.manager;

import com.example.gameserver.handler.ResponseType;
import com.example.gameserver.model.GameChoice;
import com.example.gameserver.model.GameResult;
import com.example.gameserver.model.GameRoom;
import com.example.gameserver.service.GameMessageSender;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

@Component
@Slf4j
@RequiredArgsConstructor
public class GameRoomManager {

    private final Map<String, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private final Queue<WebSocketSession> waitingPlayers = new ConcurrentLinkedQueue<>();
    private final GameMessageSender messageSender;
    private final SessionManager sessionManager;


    public void addToQueue(WebSocketSession session) {
        WebSocketSession opponent = waitingPlayers.poll();
        if (opponent == null || opponent.equals(session)) {
            waitingPlayers.offer(session);
            messageSender.sendMessage(session, ResponseType.WAITING, "Waiting for opponent...");
        } else {
            createGame(session, opponent);
        }
    }

    public boolean isPlayerInQueue(WebSocketSession session) {
        return waitingPlayers.contains(session);
    }

    public boolean isPlayerInGame(WebSocketSession session) {
        return session.getAttributes().containsKey("gameId");
    }

    public GameRoom getGameRoom(String gameId) {
        return gameRooms.get(gameId);
    }

    public void registerPlayerChoice(GameRoom gameRoom, String playerId, GameChoice choice) {
        if (playerId.equals(gameRoom.getPlayer1Id())) {
            gameRoom.setPlayer1Choice(choice);
        } else if (playerId.equals(gameRoom.getPlayer2Id())) {
            gameRoom.setPlayer2Choice(choice);
        }
    }

    public void handleGameResult(GameRoom gameRoom, GameResult result) {
        WebSocketSession player1Session = sessionManager.getSession(gameRoom.getPlayer1Id());
        WebSocketSession player2Session = sessionManager.getSession(gameRoom.getPlayer2Id());

        if (result.isDraw()) {
            String drawMessage = String.format("Draw! Both chose %s", gameRoom.getPlayer1Choice());
            messageSender.sendMessage(player1Session, ResponseType.GAME_RESULT, drawMessage);
            messageSender.sendMessage(player2Session, ResponseType.GAME_RESULT, drawMessage);
        } else {
            WebSocketSession winnerSession = sessionManager.getSession(result.getWinnerId());
            WebSocketSession loserSession = sessionManager.getSession(result.getLoserId());

            String winnerMessage = String.format("You won! %s beats %s",
                result.getWinnerChoice(), result.getLoserChoice());
            String loserMessage = String.format("You lost! %s is beaten by %s",
                result.getLoserChoice(), result.getWinnerChoice());

            messageSender.sendMessage(winnerSession, ResponseType.GAME_RESULT, winnerMessage);
            messageSender.sendMessage(loserSession, ResponseType.GAME_RESULT, loserMessage);
        }

        // 게임 종료 처리
        endGame(gameRoom);
    }

    private void createGame(WebSocketSession player1, WebSocketSession player2) {
        String gameId = UUID.randomUUID().toString();
        String player1Id = (String) player1.getAttributes().get("playerId");
        String player2Id = (String) player2.getAttributes().get("playerId");

        GameRoom gameRoom = new GameRoom(gameId, player1Id, player2Id);
        gameRooms.put(gameId, gameRoom);

        player1.getAttributes().put("gameId", gameId);
        player2.getAttributes().put("gameId", gameId);

        messageSender.sendMessage(player1, ResponseType.GAME_START,
            "Game started! Make your choice (ROCK/PAPER/SCISSORS)");
        messageSender.sendMessage(player2, ResponseType.GAME_START,
            "Game started! Make your choice (ROCK/PAPER/SCISSORS)");
    }

    private void endGame(GameRoom gameRoom) {
        WebSocketSession player1Session = sessionManager.getSession(gameRoom.getPlayer1Id());
        WebSocketSession player2Session = sessionManager.getSession(gameRoom.getPlayer2Id());

        String endMessage = "Game ended. Type JOIN_QUEUE to play again or EXIT to leave";
        messageSender.sendMessage(player1Session, ResponseType.GAME_END, endMessage);
        messageSender.sendMessage(player2Session, ResponseType.GAME_END, endMessage);

        // 게임룸 정리
        cleanupGame(gameRoom);
    }

    private void cleanupGame(GameRoom gameRoom) {
        // 세션에서 게임 ID 제거
        WebSocketSession player1Session = sessionManager.getSession(gameRoom.getPlayer1Id());
        WebSocketSession player2Session = sessionManager.getSession(gameRoom.getPlayer2Id());

        if (player1Session != null) {
            player1Session.getAttributes().remove("gameId");
        }
        if (player2Session != null) {
            player2Session.getAttributes().remove("gameId");
        }

        // 게임룸 제거
        gameRooms.remove(gameRoom.getGameId());
    }

    public void handlePlayerDisconnect(String playerId) {
        // 대기열에서 제거
        waitingPlayers.removeIf(session ->
            playerId.equals(session.getAttributes().get("playerId")));

        // 게임 중이었다면 상대방에게 알림
        gameRooms.values().stream()
            .filter(room -> room.hasPlayer(playerId))
            .findFirst()
            .ifPresent(room -> {
                String opponentId = room.getOpponentId(playerId);
                WebSocketSession opponentSession = sessionManager.getSession(opponentId);

                if (opponentSession != null) {
                    messageSender.sendMessage(opponentSession,
                        ResponseType.OPPONENT_DISCONNECTED,
                        "Opponent disconnected. Game ended.");
                }

                cleanupGame(room);
            });

        // 세션 제거
        sessionManager.removeSession(playerId);
    }
}
