package com.go.server.game.session.repository;

import com.go.server.game.session.model.Session;
import com.go.server.game.session.repository.document.SessionDocument;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Repository
public class SessionRepository {
    private static char DOCUMENT_NAMESPACE_SEPARATOR = '/';
    private final Logger logger = LoggerFactory.getLogger(SessionRepository.class);
    final private Firestore firestore;

    public SessionRepository(@NonNull final Firestore firestore) {
        this.firestore = firestore;
    }

    public List<Session> getAllSessions() {
        try {
            return firestore
                    .collection(SessionDocument.COLLECTION_NAME)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(mapping -> SessionDocument.toSession(mapping.toObject(SessionDocument.class)))
                    .toList();
        } catch (ExecutionException | InterruptedException error) {
            logger.error("Error during requesting all sessions: " + error.getMessage());
            return List.of();
        }
    }

    public void addSession(final Session session) {
        final var document = SessionDocument.fromSession(session);

        try {
            // TODO: Check if session already exists
            firestore
                    .document(SessionDocument.COLLECTION_NAME + DOCUMENT_NAMESPACE_SEPARATOR + document.id)
                    .create(document)
                    .get();
        } catch (ExecutionException | InterruptedException error) {
            logger.error("Error during adding session: " + error.getMessage());
        }
    }

    public void removeSession(final Session session) {
        try {
            firestore
                    .collection(SessionDocument.COLLECTION_NAME)
                    .document(session.getId())
                    .delete()
                    .get();
        } catch (ExecutionException | InterruptedException error) {
            logger.error("Error during removing session: " + error.getMessage());
        }
    }

    public void removeSessions(final List<Session> sessions) {
        final var sessionIds = sessions.stream().map(Session::getId).toList();

        try {
            firestore
                    .collection(SessionDocument.COLLECTION_NAME)
                    .whereIn(SessionDocument.ID_FIELD_NAME, sessionIds)
                    .get()
                    .get()
                    .getDocuments()
                    .forEach(mapping -> firestore
                            .collection(SessionDocument.COLLECTION_NAME)
                            .document(mapping.getId())
                            .delete());
        } catch (ExecutionException | InterruptedException error) {
            logger.error("Error during removing sessions: " + error.getMessage());
        }
    }

    public Session getSession(final String sessionId) {
        try {
            final var document = firestore
                    .collection(SessionDocument.COLLECTION_NAME)
                    .document(sessionId)
                    .get()
                    .get()
                    .toObject(SessionDocument.class);

            if (document != null) {
                return SessionDocument.toSession(document);
            }

            return Session.notFound(sessionId);
        } catch (ExecutionException | InterruptedException error) {
            logger.error("Error during requesting session: " + error.getMessage());
            return Session.notFound(sessionId);
        }
    }

    public Session updateSession(final Session session) {
        final var document = SessionDocument.fromSession(session);

        firestore
                .collection(SessionDocument.COLLECTION_NAME)
                .document(document.id)
                .set(document);

        return session;
    }

    public Optional<Session> getSessionByPlayerId(final UUID playerId) {
        try {
            return firestore
                    .collection(SessionDocument.COLLECTION_NAME)
                    .whereArrayContains(SessionDocument.PLAYER_IDS_FIELD_NAME, playerId.toString())
                    .get()
                    .get()
                    .toObjects(SessionDocument.class)
                    .stream()
                    .findFirst()
                    .map(SessionDocument::toSession);
        } catch (ExecutionException | InterruptedException error) {
            logger.error("Error during requesting player session: " + error.getMessage());
            return Optional.empty();
        }
    }
}
