package com.example.gameserver.model;

import lombok.Data;

@Data
public class GameRoom {
    private final String gameId;
    private final String player1Id;
    private final String player2Id;
    private GameChoice player1Choice;
    private GameChoice player2Choice;
    private boolean isGameFinished = false;

    public boolean isBothPlayersSelected() {
        return player1Choice != null && player2Choice != null;
    }

    public boolean hasPlayer(String playerId) {
        return player1Id.equals(playerId) || player2Id.equals(playerId);
    }

    public String getOpponentId(String playerId) {
        if (player1Id.equals(playerId)) {
            return player2Id;
        } else if (player2Id.equals(playerId)) {
            return player1Id;
        }
        throw new IllegalArgumentException("Player not in this game room");
    }
}