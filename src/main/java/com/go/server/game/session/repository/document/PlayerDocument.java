package com.go.server.game.session.repository.document;

import com.go.server.game.session.model.Colors;
import com.go.server.game.session.model.Player;

import java.util.UUID;

public class PlayerDocument {
    public String id;
    public Colors color;
    public boolean isBot;

    public PlayerDocument() {
    }

    public PlayerDocument(final String id, final Colors color, final boolean isBot) {
        this.id = id;
        this.color = color;
        this.isBot = isBot;
    }

    static public PlayerDocument fromPlayer(final Player player) {
        return new PlayerDocument(player.getId().toString(), player.getColor(), player.isBot());
    }

    static public Player toPlayer(final PlayerDocument document) {
        UUID uuid = UUID.fromString(document.id);
        return document.isBot ? Player.bot(uuid, document.color) : Player.human(uuid, document.color);
    }
}
