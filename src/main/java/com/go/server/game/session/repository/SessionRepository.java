package com.go.server.game.session.repository;

import com.go.server.game.session.model.Session;
import com.go.server.game.session.repository.document.SessionDocument;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.concurrent.ExecutionException;
import com.go.server.game.session.exception.SessionNotFoundException;

@Repository
public class SessionRepository {
    private final static char DOCUMENT_NAMESPACE_SEPARATOR = '/';
    private final Logger logger = LoggerFactory.getLogger(SessionRepository.class);
    private final Firestore firestore;

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
            logger.error("Error during requesting all sessions: {}", error.getMessage());
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
            logger.error("Error during adding session: {}", error.getMessage());
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
            logger.error("Error during removing session: {}", error.getMessage());
        }
    }

    public void removeSessions(final List<Session> sessions) {
        final int batchSize = 30; // Firestore limit for 'IN' queries

        IntStream.iterate(0, i -> i < sessions.size(), i -> i + batchSize)
                .mapToObj(i -> sessions.subList(i, Math.min(i + batchSize, sessions.size())))
                .forEach(this::removeSessionBatch);
    }

    private void removeSessionBatch(final List<Session> batch) {
        final var sessionIds = batch.stream().map(Session::getId).toList();

        try {
            final var documents = firestore
                    .collection(SessionDocument.COLLECTION_NAME)
                    .whereIn(FieldPath.documentId(), sessionIds)
                    .get()
                    .get()
                    .getDocuments();

            if (!documents.isEmpty()) {
                final var batchWrite = firestore.batch();
                documents.forEach(doc -> batchWrite.delete(doc.getReference()));
                batchWrite.commit().get();
            }
        } catch (ExecutionException | InterruptedException error) {
            logger.error("Error during removing sessions batch: {}", error.getMessage());
        }
    }

    public Optional<Session> findSession(final String sessionId) {
        try {
            final var document = firestore
                    .collection(SessionDocument.COLLECTION_NAME)
                    .document(sessionId)
                    .get()
                    .get()
                    .toObject(SessionDocument.class);

            return Optional.ofNullable(document).map(SessionDocument::toSession);
        } catch (ExecutionException | InterruptedException error) {
            logger.error("Error during requesting session: {}", error.getMessage());
            return Optional.empty();
        }
    }

    public Session getSession(final String sessionId) {
        return findSession(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    public Session updateSession(final Session session) {
        final var document = SessionDocument.fromSession(session);

        try {
            firestore
                    .collection(SessionDocument.COLLECTION_NAME)
                    .document(document.id)
                    .set(document)
                    .get();
        } catch (ExecutionException | InterruptedException error) {
            logger.error("Error during updating session: {}", error.getMessage());
        }
        


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
            logger.error("Error during requesting player session: {}", error.getMessage());
            return Optional.empty();
        }
    }
}
