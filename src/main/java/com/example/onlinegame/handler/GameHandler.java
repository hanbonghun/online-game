package com.example.onlinegame.handler;

import com.example.onlinegame.model.GameChoice;
import com.example.onlinegame.model.GameResult;
import com.example.onlinegame.model.GameRoom;
import com.example.onlinegame.service.GameService;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    private final GameService gameService;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Queue<WebSocketSession> waitingPlayers = new ConcurrentLinkedQueue<>();
    private final Map<String, GameRoom> gameRooms = new ConcurrentHashMap<>();


    @Override
    public Mono<Void> handle(WebSocketSession session) {
        log.info("New WebSocket connection attempt");

        return session.receive()
            .doOnSubscribe(sub -> {
                String playerId = generatePlayerId();
                log.info("Player connected: {}", playerId);
                sessions.put(playerId, session);
                session.getAttributes().put("playerId", playerId);
                findMatch(session);
            })
            .doOnNext(message -> {
                log.info("Received message: {}", message.getPayloadAsText());
                handleGameMessage(session, message);
            })
            .doOnError(error -> {
                log.error("WebSocket error: ", error);
            })
            .doFinally(signalType -> {
                log.info("Connection closing: {}", signalType);
                handleDisconnect(session);
            })
            .then();
    }


    private void handleGameMessage(WebSocketSession session, WebSocketMessage message) {
        if (message.getType() == WebSocketMessage.Type.TEXT) {
            String payload = message.getPayloadAsText();
            try {
                GameChoice choice = GameChoice.valueOf(payload.toUpperCase());
                processGameChoice(session, choice);
            } catch (IllegalArgumentException e) {
                sendMessage(session, "Invalid choice! Please choose ROCK, PAPER, or SCISSORS");
            }
        }
    }

    private void processGameChoice(WebSocketSession session, GameChoice choice) {
        String playerId = (String) session.getAttributes().get("playerId");
        String gameId = (String) session.getAttributes().get("gameId");

        GameRoom gameRoom = gameRooms.get(gameId);
        if (gameRoom != null) {
            if (playerId.equals(gameRoom.getPlayer1Id())) {
                gameRoom.setPlayer1Choice(choice);
                sendMessage(session, "You chose " + choice);
            } else if (playerId.equals(gameRoom.getPlayer2Id())) {
                gameRoom.setPlayer2Choice(choice);
                sendMessage(session, "You chose " + choice);
            }

            if (gameRoom.isBothPlayersSelected() && !gameRoom.isGameFinished()) {
                determineWinner(gameRoom);
            }
        }
    }

    private String generatePlayerId() {
        return UUID.randomUUID().toString();
    }

    private void findMatch(WebSocketSession session) {
        WebSocketSession opponent = waitingPlayers.poll();
        if (opponent == null || opponent.equals(session)) {
            waitingPlayers.offer(session);
            sendMessage(session, "Waiting for opponent...");
        } else {
            startGame(session, opponent);
        }
    }

    private void startGame(WebSocketSession player1, WebSocketSession player2) {
        String gameId = UUID.randomUUID().toString();
        String player1Id = (String) player1.getAttributes().get("playerId");
        String player2Id = (String) player2.getAttributes().get("playerId");

        // 게임룸 생성
        GameRoom gameRoom = new GameRoom(gameId, player1Id, player2Id);
        System.out.println("룸 생성. 참여 플레이어 아이디 : " + player1Id + ", " + player2Id);
        gameRooms.put(gameId, gameRoom);

        // 세션에 게임 ID 저장
        player1.getAttributes().put("gameId", gameId);
        player2.getAttributes().put("gameId", gameId);

        // 게임 시작 메시지 전송
        sendMessage(player1, "Game started! Make your choice (ROCK/PAPER/SCISSORS)");
        sendMessage(player2, "Game started! Make your choice (ROCK/PAPER/SCISSORS)");
    }

    private void determineWinner(GameRoom gameRoom) {
        GameResult result = gameService.determineWinner(
            gameRoom.getPlayer1Id(), gameRoom.getPlayer1Choice(),
            gameRoom.getPlayer2Id(), gameRoom.getPlayer2Choice()
        );

        // 결과 전송
        WebSocketSession player1Session = sessions.get(gameRoom.getPlayer1Id());
        WebSocketSession player2Session = sessions.get(gameRoom.getPlayer2Id());

        if (result.isDraw()) {
            sendMessage(player1Session, "Draw! Both chose " + gameRoom.getPlayer1Choice());
            sendMessage(player2Session, "Draw! Both chose " + gameRoom.getPlayer2Choice());
        } else {
            WebSocketSession winnerSession = sessions.get(result.getWinnerId());
            WebSocketSession loserSession = sessions.get(result.getLoserId());

            sendMessage(winnerSession,
                "You won! " + result.getWinnerChoice() + " beats " + result.getLoserChoice());
            sendMessage(loserSession, "You lost! " + result.getLoserChoice() + " is beaten by "
                + result.getWinnerChoice());
        }

        // 게임 종료 처리
        gameRoom.setGameFinished(true);

        // 새 라운드 시작 또는 게임 종료 처리
        handleGameEnd(gameRoom);
    }

    private void handleGameEnd(GameRoom gameRoom) {
        WebSocketSession player1Session = sessions.get(gameRoom.getPlayer1Id());
        WebSocketSession player2Session = sessions.get(gameRoom.getPlayer2Id());

        // 새 게임을 위한 선택지 제공
        String nextRoundMessage = "Play again? Type 'YES' for new round or 'EXIT' to leave";
        sendMessage(player1Session, nextRoundMessage);
        sendMessage(player2Session, nextRoundMessage);
    }

    private void sendMessage(WebSocketSession session, String message) {
        session.send(Mono.just(session.textMessage(message))).subscribe();
    }

    private void handleDisconnect(WebSocketSession session) {
        String playerId = (String) session.getAttributes().get("playerId");
        String gameId = (String) session.getAttributes().get("gameId");

        sessions.remove(playerId);
        waitingPlayers.remove(session);

        if (gameId != null) {
            GameRoom gameRoom = gameRooms.get(gameId);
            if (gameRoom != null) {
                notifyOpponentOfDisconnect(gameRoom, playerId);
                gameRooms.remove(gameId);
            }
        }
    }

    private void notifyOpponentOfDisconnect(GameRoom gameRoom, String disconnectedPlayerId) {
        String otherPlayerId = gameRoom.getPlayer1Id().equals(disconnectedPlayerId)
            ? gameRoom.getPlayer2Id()
            : gameRoom.getPlayer1Id();

        WebSocketSession otherSession = sessions.get(otherPlayerId);
        if (otherSession != null) {
            sendMessage(otherSession, "Opponent disconnected. Game ended.");
        }
    }
}
