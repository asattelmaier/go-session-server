package com.go.server.user.model;

import com.go.server.user.model.output.UserDto;

import java.util.UUID;

public class User {
    private final UUID id;
    private final String username;

    public User(
            final UUID id,
            final String username
    ) {
        this.id = id;
        this.username = username;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public UserDto toDto() {
        return new UserDto(id.toString(), username);
    }
}
