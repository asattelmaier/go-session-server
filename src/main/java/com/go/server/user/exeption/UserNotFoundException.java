package com.go.server.user.exeption;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(final UUID userId) {
        super("User with id \"" + userId.toString() + "\" not found");
    }
}
