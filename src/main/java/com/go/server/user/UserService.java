package com.go.server.user;

import com.go.server.user.exception.InvalidUserIdException;
import com.go.server.user.model.User;
import com.go.server.user.model.output.UserDto;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
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
