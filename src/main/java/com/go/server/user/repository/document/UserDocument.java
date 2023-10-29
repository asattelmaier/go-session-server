package com.go.server.user.repository.document;

import com.go.server.user.model.User;

import java.util.UUID;

public class UserDocument {
    public static String COLLECTION_NAME = "users";
    public String id;
    public String username;

    private UserDocument(final String id, final String username) {
        this.id = id;
        this.username = username;
    }

    public static UserDocument fromUser(final User user) {
        return new UserDocument(user.getId().toString(), user.getUsername());
    }

    public static User toUser(final UserDocument user) {
        return new User(UUID.fromString(user.id), user.username);
    }
}
