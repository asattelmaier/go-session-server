package com.go.server.game.session.repository.document;

import com.go.server.game.session.model.BotDifficulty;
import com.go.server.game.session.model.Session;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import org.slf4j.LoggerFactory;

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
    public BotDifficulty difficulty;
    public Integer boardSize; // Use Integer to detect missing values
    public List<String> moves;
    public Timestamp updated;

    public SessionDocument() {
    }

    public SessionDocument(final String id, final List<PlayerDocument> players, final BotDifficulty difficulty, final Integer boardSize, final List<String> moves, final Instant updated) {
        this.id = id;
        this.players = players;
        this.playerIds = players.stream().map(player -> player.id).toList();
        this.difficulty = difficulty;
        this.boardSize = boardSize;
        this.moves = moves;
        this.updated = Timestamp.of(Date.from(updated));
    }

    public static SessionDocument fromSession(final Session session) {
        final var players = session.getPlayers().stream().map(PlayerDocument::fromPlayer).toList();

        return new SessionDocument(session.getId(), players, session.getDifficulty().orElse(null), session.getBoardSize(), session.getMoves(), session.getUpdated());
    }

    public static Session toSession(final SessionDocument document) {
        final var players = document.players.stream().map(PlayerDocument::toPlayer).toList();
        final var session = new Session(document.id, document.updated.toDate().toInstant(), players);
        session.setDifficulty(document.difficulty);
        
        // Default to 19 if missing or 0
        int size = (document.boardSize == null || document.boardSize == 0) ? 19 : document.boardSize;
        session.setBoardSize(size);
        
        if (document.moves != null) {
            document.moves.forEach(session::addMove);
        }
        
        LoggerFactory.getLogger(SessionDocument.class).info("Loaded session {} from document. boardSize: {}, moves.size: {}", 
                session.getId(), session.getBoardSize(), (document.moves != null ? document.moves.size() : 0));
                
        return session;
    }
}
