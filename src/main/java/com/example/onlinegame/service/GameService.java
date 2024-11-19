package com.example.onlinegame.service;

import com.example.onlinegame.model.GameChoice;
import com.example.onlinegame.model.GameResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {

    public GameResult determineWinner(String player1Id, GameChoice player1Choice,
        String player2Id, GameChoice player2Choice) {
        if (player1Choice == player2Choice) {
            return new GameResult(null, null, player1Choice, player2Choice, true);
        }

        boolean player1Wins = isWinner(player1Choice, player2Choice);

        if (player1Wins) {
            return new GameResult(player1Id, player2Id, player1Choice, player2Choice, false);
        } else {
            return new GameResult(player2Id, player1Id, player2Choice, player1Choice, false);
        }
    }

    private boolean isWinner(GameChoice choice1, GameChoice choice2) {
        return (choice1 == GameChoice.ROCK && choice2 == GameChoice.SCISSORS) ||
            (choice1 == GameChoice.SCISSORS && choice2 == GameChoice.PAPER) ||
            (choice1 == GameChoice.PAPER && choice2 == GameChoice.ROCK);
    }
}