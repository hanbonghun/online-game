
package com.example.gameserver.common;

import com.example.gameserver.handler.GameCommand;

public interface GameCommandHandler {
    GameCommand getCommand();
}