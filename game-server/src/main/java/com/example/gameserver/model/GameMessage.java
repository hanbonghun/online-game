package com.example.gameserver.model;

import com.example.gameserver.handler.GameCommand;
import lombok.Data;

@Data
public class GameMessage<T> {
    private GameCommand command;
    private T data;
}