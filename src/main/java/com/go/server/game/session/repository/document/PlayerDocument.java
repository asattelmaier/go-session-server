package com.go.server.game.session.repository.document;

import com.go.server.game.session.model.Colors;
import com.go.server.game.session.model.Player;

import java.util.UUID;

public class PlayerDocument {
    public String id;
    public Colors color;

    public PlayerDocument() {
    }

    public PlayerDocument(final String id, final Colors color) {
        this.id = id;
        this.color = color;
    }

    static public PlayerDocument fromPlayer(final Player player) {
        return new PlayerDocument(player.getId().toString(), player.getColor());
    }

    static public Player toPlayer(final PlayerDocument document) {
        return new Player(UUID.fromString(document.id), document.color);
    }
}
