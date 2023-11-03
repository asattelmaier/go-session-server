package com.go.server.user.exception;

public class InvalidUserIdException extends RuntimeException {
    public InvalidUserIdException(final String userId) {
        super("Invalid user id \"" + userId + "\"");
    }
}
