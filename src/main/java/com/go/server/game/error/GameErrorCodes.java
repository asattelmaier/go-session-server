package com.go.server.game.error;

public final class GameErrorCodes {
    public static final String SESSION_FULL = "SESSION_FULL";
    public static final String INVALID_USER_ID = "INVALID_USER_ID";
    public static final String SESSION_NOT_FOUND = "SESSION_NOT_FOUND";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String INVALID_MOVE = "INVALID_MOVE";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    private GameErrorCodes() {
        // Prevent instantiation
    }
}
