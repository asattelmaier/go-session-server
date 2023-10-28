package com.go.server.game.session.repository.document;

import com.go.server.game.session.model.Session;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;

import java.sql.Date;
import java.time.Instant;
import java.util.List;

public class SessionDocument {
    public static String COLLECTION_NAME = "sessions";
    public static String PLAYER_IDS_FIELD_NAME = "playerIds";
    public static String ID_FIELD_NAME = "id";
    @DocumentId
    public String id;
    public List<PlayerDocument> players;
    public List<String> playerIds;
    public Timestamp updated;

    public SessionDocument() {
    }

    public SessionDocument(final String id, final List<PlayerDocument> players, final Instant updated) {
        this.id = id;
        this.players = players;
        this.playerIds = players.stream().map(player -> player.id).toList();
        this.updated = Timestamp.of(Date.from(updated));
    }

    public static SessionDocument fromSession(final Session session) {
        final var players = session.getPlayers().stream().map(PlayerDocument::fromPlayer).toList();

        return new SessionDocument(session.getId(), players, session.getUpdated());
    }

    public static Session toSession(final SessionDocument document) {
        final var players = document.players.stream().map(PlayerDocument::toPlayer).toList();

        return new Session(document.id, Instant.now(), players);
    }
}
