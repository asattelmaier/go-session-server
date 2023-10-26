package com.go.server.user.exeption;

public class InvalidUserIdException extends RuntimeException {
    public InvalidUserIdException(final String userId) {
        super("Invalid user id \"" + userId + "\"");
    }
}
