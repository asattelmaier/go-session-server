package com.go.server.game.session.model;

import com.go.server.game.session.model.output.PlayerDto;

import java.util.UUID;

public class Player {
    private final UUID id;
    private final Colors color;
    private final boolean isBot;

    private Player(final UUID id, final Colors color, final boolean isBot) {
        this.id = id;
        this.color = color;
        this.isBot = isBot;
    }

    public static Player human(final UUID id, final Colors color) {
        return new Player(id, color, false);
    }

    public static Player bot(final UUID id, final Colors color) {
        return new Player(id, color, true);
    }


    public UUID getId() {
        return id;
    }

    public Colors getColor() {
        return color;
    }

    public boolean isBot() {
        return isBot;
    }

    public PlayerDto toDto() {
        if (color == Colors.BLACK) {
            return new PlayerDto(id.toString(), "Black", isBot);
        }

        return new PlayerDto(id.toString(), "White", isBot);
    }
}
