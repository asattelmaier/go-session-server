package com.go.server.user.repository.document;

import com.go.server.user.model.User;
import com.google.cloud.firestore.annotation.DocumentId;

import java.util.UUID;

public class UserDocument {
    public static String COLLECTION_NAME = "users";
    @DocumentId
    public String id;
    public String username;
    public String password;
    public String token;

    public UserDocument() {
    }

    private UserDocument(
            final String id,
            final String username,
            final String password,
            final String token
    ) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.token = token;
    }

    public static UserDocument fromUser(final User user) {
        return new UserDocument(
                user.getId().toString(),
                user.getUsername(),
                user.getPassword(),
                user.getToken()
        );
    }

    public static User toUser(final UserDocument user) {
        return new User(UUID.fromString(user.id), user.username, user.password, user.token);
    }
}
