package com.go.server.user;

import com.go.server.user.exeption.InvalidUserIdException;
import com.go.server.user.exeption.UserNotFoundException;
import com.go.server.user.model.Guest;
import com.go.server.user.model.output.UserDto;
import com.go.server.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(final UserRepository repository) {
        this.repository = repository;
    }

    public UserDto createGuestUser() {
        final var user = new Guest(UUID.randomUUID());

        repository.createUser(user);

        return user.toDto();
    }

    public static UUID userIdFromString(final String userId) {
        try {
            return UUID.fromString(userId);
        } catch (final IllegalArgumentException e) {
            throw new InvalidUserIdException(userId);
        }
    }

    public UserDto getUser(final String userId) {
        final var userUuid = userIdFromString(userId);
        final var user = repository.getUser(UUID.fromString(userId));

        if (user.isPresent()) {
            return user.get().toDto();
        }

        throw new UserNotFoundException(userUuid);
    }
}
