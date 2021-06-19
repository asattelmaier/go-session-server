package com.go.server.game.session.model.output;

import java.util.Objects;

public class PlayerDto {
    public final String id;
    public final String color;

    public PlayerDto(final String id, final String color) {
        this.id = id;
        this.color = color;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        }

        if (!(other instanceof PlayerDto otherObject)) {
            return false;
        }

        return otherObject.id.equals(id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
