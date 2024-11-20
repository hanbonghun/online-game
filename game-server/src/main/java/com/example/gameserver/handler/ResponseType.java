package com.example.gameserver.handler;

public enum ResponseType {
    CONNECTED,
    WAITING,
    GAME_START,
    GAME_RESULT,
    GAME_END,
    ERROR,
    OPPONENT_DISCONNECTED
}