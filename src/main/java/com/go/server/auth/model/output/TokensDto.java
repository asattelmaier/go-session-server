package com.go.server.auth.model.output;

public record TokensDto(String accessToken, String refreshToken) {
}
