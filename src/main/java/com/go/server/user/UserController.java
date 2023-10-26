package com.go.server.user;

import com.go.server.user.exeption.InvalidUserIdException;
import com.go.server.user.exeption.UserNotFoundException;
import com.go.server.user.model.output.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UserController {
    private final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(@NonNull final UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user/{userId}")
    public UserDto getUser(@NonNull @PathVariable final String userId) {
        try {
            logger.info("User data requested");
            return userService.getUser(userId);
        } catch (UserNotFoundException error) {
            logger.error("Error during user data request:" + error.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Found", error);
        } catch (InvalidUserIdException | NullPointerException error) {
            logger.error("Error during user data request:" + error.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid User ID", error);
        }
    }

    @PostMapping("/user/guest")
    public UserDto createGuestUser() {
        final var userDto = userService.createGuestUser();

        logger.info("New guest user created");

        return userDto;
    }
}