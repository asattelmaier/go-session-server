package com.go.server.game.model.output;

import java.time.Instant;

public record ErrorDto(
        String code,
        String message,
        String timestamp
) {
    public ErrorDto(String code, String message) {
        this(code, message, Instant.now().toString());
    }
}
