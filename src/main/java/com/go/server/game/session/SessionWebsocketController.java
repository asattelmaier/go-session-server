package com.go.server.game.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.security.Principal;
import com.go.server.game.session.model.Session;
import com.go.server.game.model.DeviceMove;
import com.go.server.game.session.model.GameCommandType;
import com.go.server.game.session.model.input.CreateSessionDto;
import com.go.server.game.session.model.output.SessionDto;
import com.go.server.user.exception.InvalidUserIdException;
import com.go.server.user.UserService;
import com.go.server.user.model.User;
import java.util.UUID;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Controller;

@Controller
public class SessionWebsocketController {
    private final Logger logger = LoggerFactory.getLogger(SessionWebsocketController.class);
    private final SessionService sessionService;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    public SessionWebsocketController(@NonNull final SessionService sessionService,
                                      @NonNull final UserService userService) {
        this.sessionService = sessionService;
        this.userService = userService;
    }



    @MessageMapping("/create")
    @SendToUser("/game/session/created")
    public SessionDto createSession(@Payload @NonNull @Valid final CreateSessionDto createSessionDto) {
        logger.info("Create session request received: {}", createSessionDto);
        
        final var player = userService.getUserByName(createSessionDto.getPlayerId());
        final var sessionDto = sessionService.createSession(player, createSessionDto.getDifficulty(), createSessionDto.getBoardSize());
        logger.info("Session \"{}\" created", sessionDto.id);

        return sessionDto;
    }

    @MessageMapping("/{sessionId}/terminate")
    public void terminateSession(@NonNull @DestinationVariable final String sessionId) {
        logger.info("Session \"{}\" terminated", sessionId);
        this.sessionService.terminateSession(sessionId);
    }

    @MessageMapping("/{sessionId}/update")
    @SuppressWarnings("unchecked")
    public void updateSession(Principal principal, @NonNull @DestinationVariable final String sessionId, @Payload @NonNull final Map<String, Object> payload) {
        logger.info("Session \"{}\" received update payload: {}", sessionId, payload);

        if (principal == null) {
            logger.warn("Unauthenticated update attempt for session {}", sessionId);
            return;
        }
        
        final String username = principal.getName();
        final var player = userService.getUserByName(username);

        Optional.ofNullable(payload.get("command"))
                .filter(Map.class::isInstance)
                .map(cmd -> (Map<String, Object>) cmd)
                .map(cmd -> String.valueOf(cmd.get("name")))
                .map(GameCommandType::fromString)
                .ifPresentOrElse(
                        commandType -> handleCommand(sessionId, player, commandType, (Map<String, Object>) payload.get("command")),
                        () -> handleLegacyPayload(sessionId, player, payload)
                );
    }

    private void handleCommand(String sessionId, User player, GameCommandType commandType, Map<String, Object> commandPayload) {

        switch (commandType) {
            case PLAY -> {
                Optional.ofNullable(commandPayload.get("location"))
                    .map(loc -> objectMapper.convertValue(loc, DeviceMove.class))
                    .ifPresent(move -> sessionService.updateSession(sessionId, player, move));
            }
            case PASS -> sessionService.updateSession(sessionId, player, DeviceMove.pass());
            case CREATE -> sessionService.initializeGame(sessionId);
            default -> logger.warn("Unknown command received in update");
        }
    }

    private void handleLegacyPayload(String sessionId, User player, Map<String, Object> payload) {
        try {
            Optional.ofNullable(objectMapper.convertValue(payload, DeviceMove.class))
                    .filter(move -> move.getType() != null)
                    .filter(move -> move.getType() != null)
                    .ifPresent(move -> sessionService.updateSession(sessionId, player, move));
        } catch (Exception e) {
            logger.error("Failed to parse update payload for session {}: {}", sessionId, payload);
        }
    }

    @MessageMapping("/{sessionId}/join")
    @SendToUser("/game/session/joined")
    public SessionDto joinSession(java.security.Principal principal, @Header("simpSessionId") final String sessionIdHeader, @NonNull @DestinationVariable final String sessionId) {
        final String playerIdentifier = principal != null ? principal.getName() : sessionIdHeader;
        final var player = userService.getUserByName(playerIdentifier);
        logger.info("Player joins the session \"{}\" (Player ID: {})", sessionId, player.getId());
        final var sessionDto = this.sessionService.joinSession(player, sessionId);
        logger.info("Player joined the session \"{}\"", sessionId);

        return sessionDto;
    }
}