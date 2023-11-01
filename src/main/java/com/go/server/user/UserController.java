package com.go.server.user;

import com.go.server.user.exception.InvalidUserIdException;
import com.go.server.user.exception.UserNotFoundException;
import com.go.server.user.model.User;
import com.go.server.user.model.output.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/user")
public class UserController {
    private final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(@NonNull final UserService userService) {
        this.userService = userService;
    }

    @GetMapping("")
    public UserDto getUser() {
        try {
            logger.info("User data requested");
            return userService.getUser(User.getFromSecurityContext());
        } catch (UserNotFoundException error) {
            logger.error("Error during user data request:" + error.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Found", error);
        } catch (InvalidUserIdException | NullPointerException error) {
            logger.error("Error during user data request:" + error.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid User ID", error);
        }
    }
}