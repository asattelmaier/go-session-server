package com.go.server.user;

import com.go.server.user.exception.InvalidUserIdException;
import com.go.server.user.model.User;
import com.go.server.user.model.output.UserDto;
import com.go.server.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import com.go.server.user.exception.UserNotFoundException;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(final UserRepository repository) {
        this.repository = repository;
    }

    public User getUserByName(final String username) {
        return repository.findByUsername(username)
                .or(() -> {
                    try {
                        final UUID id = UUID.fromString(username);
                        return repository.getUser(id);
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                })
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    public static UUID userIdFromString(final String userId) {
        try {
            return UUID.fromString(userId);
        } catch (final IllegalArgumentException e) {
            throw new InvalidUserIdException(userId);
        }
    }

    public UserDto getUser(final User user) {
        return user.toDto();
    }
}
