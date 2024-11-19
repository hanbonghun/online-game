package com.example.onlinegame.model;

import com.example.onlinegame.handler.GameCommand;
import lombok.Data;

@Data
public class GameMessage<T> {
    private GameCommand command;
    private T data;
}