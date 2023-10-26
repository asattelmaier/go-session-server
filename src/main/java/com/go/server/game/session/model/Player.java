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

    public boolean isPlayer(final UUID id) {
        return id.equals(this.id);
    }

    public PlayerDto toDto() {
        if (color == Colors.BLACK) {
            return new PlayerDto(id.toString(), "Black");
        }

        return new PlayerDto(id.toString(), "White");
    }
}
