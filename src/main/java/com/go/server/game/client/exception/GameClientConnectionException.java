package com.go.server.game.client.exception;

public class GameClientConnectionException extends RuntimeException {
    public GameClientConnectionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
