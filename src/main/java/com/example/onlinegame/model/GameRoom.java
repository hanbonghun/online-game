package com.example.onlinegame.model;

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

    public void reset() {
        player1Choice = null;
        player2Choice = null;
        isGameFinished = false;
    }
}