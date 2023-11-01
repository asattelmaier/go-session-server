package com.go.server.auth.model.input;

public class RegisterUserDto {
    public final String username;
    public final String password;

    public RegisterUserDto(final String username, final String password) {
        this.username = username;
        this.password = password;
    }
}
