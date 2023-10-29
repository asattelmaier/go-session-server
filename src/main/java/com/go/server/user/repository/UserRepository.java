package com.go.server.user.repository;

import com.go.server.user.model.User;
import com.go.server.user.repository.document.UserDocument;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Repository
public class UserRepository {
    private final static char DOCUMENT_NAMESPACE_SEPARATOR = '/';
    private final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private final Firestore firestore;

    public UserRepository(@NonNull final Firestore firestore) {
        this.firestore = firestore;
    }

    public void createUser(final User user) {
        if (getUser(user.getId()).isPresent()) return;

        final var document = UserDocument.fromUser(user);

        try {
            firestore
                    .document(UserDocument.COLLECTION_NAME + DOCUMENT_NAMESPACE_SEPARATOR + document.id)
                    .create(document)
                    .get();
        } catch (ExecutionException | InterruptedException error) {
            logger.error("Error during user creation: " + error.getMessage());
        }
    }

    public Optional<User> getUser(final UUID userId) {
        try {
            final var document = firestore
                    .collection(UserDocument.COLLECTION_NAME)
                    .document(userId.toString())
                    .get()
                    .get()
                    .toObject(UserDocument.class);

            if (document != null) {
                return Optional.of(UserDocument.toUser(document));
            }

            return Optional.empty();
        } catch (ExecutionException | InterruptedException error) {
            logger.error("Error during requesting user: " + error.getMessage());
            return Optional.empty();
        }
    }
}
