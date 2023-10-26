package com.go.server.user;

import com.go.server.user.model.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class UserRepository {
    private final List<User> users = new CopyOnWriteArrayList<>();

    public void createUser(final User user) {
        if (getUser(user.getId()).isEmpty()) {
            users.add(user);
        }
    }

    public Optional<User> getUser(final UUID userId) {
        return users.stream()
                .filter(user -> user.getId().equals(userId))
                .findFirst();
    }
}
