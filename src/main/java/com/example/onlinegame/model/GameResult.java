package com.example.onlinegame.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GameResult {
    private String winnerId;
    private String loserId;
    private GameChoice winnerChoice;
    private GameChoice loserChoice;
    private boolean isDraw;
}