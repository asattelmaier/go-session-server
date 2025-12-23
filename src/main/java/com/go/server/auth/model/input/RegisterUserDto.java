package com.go.server.auth.model.input;

import jakarta.validation.constraints.NotBlank;

public record RegisterUserDto(@NotBlank String username, @NotBlank String password) {
}
