package com.go.server.game.session.model.output;

import java.util.List;
import java.util.Objects;

public class SessionDto {
    public final String id;
    public final List<PlayerDto> players;
    public final boolean hasError;
    public final String errorMessage;

    public SessionDto(final String id, final List<PlayerDto> players, final boolean hasError, final String errorMessage) {
        this.id = id;
        this.players = players;
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
