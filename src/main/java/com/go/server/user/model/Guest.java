package com.go.server.user.model;

import java.util.UUID;

public class Guest extends User {
    private final static String GUEST_USERNAME_PREFIX = "Guest";

    public Guest(
            final UUID id,
            final String password,
            final String token
    ) {
        super(id, createGuestUserName(id), password, token);
    }

    public static String createGuestUserName(final UUID id) {
        return GUEST_USERNAME_PREFIX + '-' + id.toString();
    }
}
