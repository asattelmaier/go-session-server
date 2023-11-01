package com.go.server.auth.model.input;

public class AuthenticateUserDto {
    public final String username;
    public final String password;

    public AuthenticateUserDto(final String username, final String password) {
        this.username = username;
        this.password = password;
    }
}
