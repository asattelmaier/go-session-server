package com.go.server.auth.exception;

import org.springframework.security.core.AuthenticationException;

public class UserAuthenticationException extends AuthenticationException {
    public UserAuthenticationException() {
        super("Authentication failed");
    }
}
