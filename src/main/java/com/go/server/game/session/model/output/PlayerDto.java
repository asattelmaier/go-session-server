package com.go.server.game.session.model.output;

import java.util.Objects;

public class PlayerDto {
    public final String id;
    public final String color;
    public final boolean isBot;

    @com.fasterxml.jackson.annotation.JsonCreator
    public PlayerDto(
            @com.fasterxml.jackson.annotation.JsonProperty("id") final String id,
            @com.fasterxml.jackson.annotation.JsonProperty("color") final String color,
            @com.fasterxml.jackson.annotation.JsonProperty("isBot") final boolean isBot
    ) {
        this.id = id;
        this.color = color;
        this.isBot = isBot;
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
