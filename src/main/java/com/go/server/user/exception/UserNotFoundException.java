package com.go.server.user.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(final String username) {
        super("User \"" + username + "\" not found");
    }
}
