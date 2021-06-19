package com.go.server.game.session.model;

import com.go.server.game.session.model.output.PlayerDto;

public class Player {
    private final String id;
    private final Colors color;

    public Player(final String id, final Colors color) {
        this.id = id;
        this.color = color;
    }

    public PlayerDto toDto() {
        if (color == Colors.BLACK) {
            return new PlayerDto(this.id, "Black");
        }

        return new PlayerDto(this.id, "White");
    }
}
