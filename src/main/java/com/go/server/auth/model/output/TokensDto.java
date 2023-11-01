package com.go.server.auth.model.output;

import com.go.server.user.model.output.UserDto;

import java.util.Objects;

public class TokensDto {
    public final String accessToken;
    public final String refreshToken;

    public TokensDto(final String accessToken, final String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
