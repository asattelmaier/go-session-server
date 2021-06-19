package com.go.server.game.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import reactor.util.annotation.NonNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class GameClientHandler extends BinaryWebSocketHandler {
    private final Logger logger = LoggerFactory.getLogger(GameClient.class);
    private final List<Function<byte[], Void>> listeners = new CopyOnWriteArrayList<>();

    public void addListener(final Function<byte[], Void> listener) {
        listeners.add(listener);
    }

    @Override
    public void handleTextMessage(@NonNull final WebSocketSession session, @NonNull final TextMessage message) {
        final var listener = listeners.stream().findFirst();

        listener.ifPresent(callback -> callback.apply(message.getPayload().getBytes()));
        listener.ifPresent(listeners::remove);
    }

    @Override
    public void afterConnectionEstablished(@NonNull final WebSocketSession session) {
        logger.info("Game Client connected" + session.getId());
    }

    @Override
    public void afterConnectionClosed(@NonNull final WebSocketSession session, @NonNull final CloseStatus status) {
        logger.info("Game Client closed: " + session.getId() + ", status: " + status.getCode() + ", reason: " + status.getReason());
    }
}
