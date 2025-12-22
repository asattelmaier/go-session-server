package com.go.server.game.session.model.output;

import java.util.List;
import java.util.Objects;

public class SessionDto {
    public final String id;
    public final List<PlayerDto> players;
    public final String difficulty;
    public final boolean hasError;
    public final String errorMessage;

    @com.fasterxml.jackson.annotation.JsonCreator
    public SessionDto(
            @com.fasterxml.jackson.annotation.JsonProperty("id") final String id,
            @com.fasterxml.jackson.annotation.JsonProperty("players") final List<PlayerDto> players,
            @com.fasterxml.jackson.annotation.JsonProperty("difficulty") final String difficulty,
            @com.fasterxml.jackson.annotation.JsonProperty("hasError") final boolean hasError,
            @com.fasterxml.jackson.annotation.JsonProperty("errorMessage") final String errorMessage
    ) {
        this.id = id;
        this.players = players;
        this.difficulty = difficulty;
        this.hasError = hasError;
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        }

        if (!(other instanceof SessionDto otherObject)) {
            return false;
        }

        return otherObject.id.equals(id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
