package com.go.server.game.session.exception;

public class SessionFullException extends RuntimeException {
    public SessionFullException(String message) {
        super(message);
    }
}
