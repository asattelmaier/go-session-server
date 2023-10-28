package com.go.server.game.session.model;

import com.go.server.game.session.model.output.PlayerDto;

import java.util.UUID;

public class Player {
    private final UUID id;
    private final Colors color;

    public Player(final UUID id, final Colors color) {
        this.id = id;
        this.color = color;
    }

    public UUID getId() {
        return id;
    }

    public Colors getColor() {
        return color;
    }

    public PlayerDto toDto() {
        if (color == Colors.BLACK) {
            return new PlayerDto(id.toString(), "Black");
        }

        return new PlayerDto(id.toString(), "White");
    }
}
