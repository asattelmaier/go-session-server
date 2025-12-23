package com.go.server.game.session.exception;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(String sessionId) {
        super("Session not found: " + sessionId);
    }
}
