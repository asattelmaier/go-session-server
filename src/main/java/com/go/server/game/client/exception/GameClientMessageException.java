package com.go.server.game.client.exception;

public class GameClientMessageException extends RuntimeException {
    public GameClientMessageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
