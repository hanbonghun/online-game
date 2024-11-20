package com.example.gameserver.common;

import com.example.gameserver.common.GameCommandHandler;
import com.example.gameserver.handler.GameCommand;
import com.example.gameserver.handler.PayloadCommandHandler;
import com.example.gameserver.handler.SimpleCommandHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;


@Slf4j
@Component
public class GameCommandDispatcher {

    private final ObjectMapper objectMapper;
    private final Map<GameCommand, GameCommandHandler> handlers;

    public GameCommandDispatcher(ObjectMapper objectMapper, List<GameCommandHandler> handlerList) {
        this.objectMapper = objectMapper;
        this.handlers = handlerList.stream()
            .collect(Collectors.toMap(
                GameCommandHandler::getCommand,
                handler -> handler
            ));
    }

    @SuppressWarnings("unchecked")
    public void dispatch(WebSocketSession session, String messageJson) {
        try {
            JsonNode jsonNode = objectMapper.readTree(messageJson);
            GameCommand command = GameCommand.valueOf(jsonNode.get("command").asText());

            GameCommandHandler handler = handlers.get(command);
            if (handler == null) {
                throw new IllegalArgumentException("Unknown command: " + command);
            }

            if (handler instanceof PayloadCommandHandler) {
                PayloadCommandHandler payloadHandler = (PayloadCommandHandler) handler;
                JsonNode dataNode = jsonNode.get("data");
                if (dataNode == null || dataNode.isNull()) {
                    throw new IllegalArgumentException("Data is required for command: " + command);
                }
                Object data = objectMapper.treeToValue(dataNode, payloadHandler.getDataType());

                ((PayloadCommandHandler<Object>) payloadHandler).handle(session, data);
            } else if (handler instanceof SimpleCommandHandler) {
                ((SimpleCommandHandler) handler).handle(session);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse message: {}", messageJson, e);
        }
    }
}