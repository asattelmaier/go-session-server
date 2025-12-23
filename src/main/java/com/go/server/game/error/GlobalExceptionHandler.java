package com.go.server.game.error;

import com.go.server.game.model.output.ErrorDto;
import com.go.server.game.session.exception.InvalidMoveException;
import com.go.server.game.session.exception.SessionNotFoundException;
import com.go.server.game.session.exception.SessionFullException;
import com.go.server.user.exception.InvalidUserIdException;
import com.go.server.user.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

import static com.go.server.game.error.GameErrorCodes.*;
import static com.go.server.configuration.WebSocketConfigConstants.ERROR_QUEUE;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @MessageExceptionHandler(SessionFullException.class)
    @SendToUser(ERROR_QUEUE)
    public ErrorDto handleSessionFullException(SessionFullException e) {
        logger.warn("Session Full Error: {}", e.getMessage());
        return new ErrorDto(SESSION_FULL, e.getMessage());
    }

    @MessageExceptionHandler(InvalidUserIdException.class)
    @SendToUser(ERROR_QUEUE)
    public ErrorDto handleInvalidUserIdException(InvalidUserIdException e) {
        logger.warn("Invalid User ID: {}", e.getMessage());
        return new ErrorDto(INVALID_USER_ID, e.getMessage());
    }

    @MessageExceptionHandler(UserNotFoundException.class)
    @SendToUser(ERROR_QUEUE)
    public ErrorDto handleUserNotFoundException(UserNotFoundException e) {
        logger.warn("User Not Found: {}", e.getMessage());
        return new ErrorDto(USER_NOT_FOUND, e.getMessage());
    }

    @MessageExceptionHandler(SessionNotFoundException.class)
    @SendToUser(ERROR_QUEUE)
    public ErrorDto handleSessionNotFoundException(SessionNotFoundException e) {
        logger.warn("Session Not Found: {}", e.getMessage());
        return new ErrorDto(SESSION_NOT_FOUND, e.getMessage());
    }

    @MessageExceptionHandler(InvalidMoveException.class)
    @SendToUser(ERROR_QUEUE)
    public ErrorDto handleInvalidMoveException(InvalidMoveException e) {
        logger.warn("Invalid Move: {}", e.getMessage());
        return new ErrorDto(INVALID_MOVE, e.getMessage());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(ERROR_QUEUE)
    public ErrorDto handleValidationException(MethodArgumentNotValidException e) {
        logger.warn("Validation Error: {}", e.getMessage());
        return new ErrorDto(BAD_REQUEST, "Validation Error: " + e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser(ERROR_QUEUE)
    public ErrorDto handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Illegal Argument: {}", e.getMessage());
        return new ErrorDto(BAD_REQUEST, e.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(ERROR_QUEUE)
    public ErrorDto handleGlobalException(Exception e) {
        logger.error("Unexpected Error", e);
        return new ErrorDto(INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }
}
