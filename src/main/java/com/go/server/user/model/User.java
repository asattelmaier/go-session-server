package com.go.server.user.model;

import com.go.server.auth.exception.UserAuthenticationException;
import com.go.server.auth.user_details.CustomUserDetails;
import com.go.server.user.model.output.UserDto;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class User {
    private final UUID id;
    private final String username;
    private final String password;
    private final String role = "USER";

    // TODO: Add Token issued
    private String token;

    public User(
            final UUID id,
            final String username,
            final String password,
            final String token
    ) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.token = token;
    }

    public static User getFromSecurityContext() {
        final var userDetails = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (userDetails instanceof CustomUserDetails) {
            return ((CustomUserDetails) userDetails).getUser();
        }

        throw new UserAuthenticationException();
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public void clearToken() {
        token = "";
    }

    public UserDto toDto() {
        return new UserDto(id.toString(), username);
    }
}
