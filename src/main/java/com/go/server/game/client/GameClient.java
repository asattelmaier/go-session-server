package com.go.server.game.client;

import com.go.server.configuration.SessionConfig;
import com.go.server.game.client.exception.GameClientConnectionException;
import com.go.server.game.client.exception.GameClientMessageException;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import javax.websocket.ContainerProvider;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class GameClient {
    private final static int MESSAGE_BUFFER_SIZE = 1000 * 1024;
    private final WebSocketSession webSocketSession;
    private final GameClientHandler gameClientHandler;

    private GameClient(final WebSocketSession webSocketSession, final GameClientHandler gameClientHandler) {
        this.webSocketSession = webSocketSession;
        this.gameClientHandler = gameClientHandler;
    }

    public static GameClient connect(final SessionConfig sessionConfig) {
        final var webSocketClient = createWebSocketClient();
        final var gameClientHandler = new GameClientHandler();
        final var webSocketSession = connect(webSocketClient, gameClientHandler, sessionConfig.getGameClientSocketUrl());

        return new GameClient(webSocketSession, gameClientHandler);
    }

    public byte[] send(final byte[] message) {
        final var response = getGameClientResponse();

        try {
            webSocketSession.sendMessage(new BinaryMessage(message));

            return response.get();
        } catch (final IOException | InterruptedException | ExecutionException e) {
            throw new GameClientMessageException("Error during message handling handling with Game Client: " + e.getMessage(), e);
        }
    }

    private Future<byte[]> getGameClientResponse() {
        final CompletableFuture<byte[]> response = new CompletableFuture<>();

        gameClientHandler.addListener((byte[] message) -> {
            response.complete(message);
            return null;
        });

        return response;
    }

    private static WebSocketClient createWebSocketClient() {
        final var container = ContainerProvider.getWebSocketContainer();

        container.setDefaultMaxBinaryMessageBufferSize(MESSAGE_BUFFER_SIZE);
        container.setDefaultMaxTextMessageBufferSize(MESSAGE_BUFFER_SIZE);

        return new StandardWebSocketClient(container);
    }

    private static @NonNull
    WebSocketSession connect(final WebSocketClient client, final GameClientHandler handler, final URI uri) {
        try {
            return client.doHandshake(handler, new WebSocketHttpHeaders(), uri).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw new GameClientConnectionException("Error during connection Handshake with Game Client: " + e.getMessage(), e);
        }
    }
}
