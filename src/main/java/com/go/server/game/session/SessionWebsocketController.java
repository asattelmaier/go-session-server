package com.go.server.game.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.go.server.game.session.model.Session;
import com.go.server.game.model.DeviceMove;
import com.go.server.game.session.model.GameCommandType;
import com.go.server.game.session.model.input.CreateSessionDto;
import com.go.server.game.session.model.output.SessionDto;
import com.go.server.user.exception.InvalidUserIdException;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<GameCommandType, BiConsumer<String, Map<String, Object>>> commandHandlers;

    public SessionWebsocketController(@NonNull final SessionService sessionService) {
        this.sessionService = sessionService;
        this.commandHandlers = new EnumMap<>(GameCommandType.class);
        initializeCommandHandlers();
    }

    private void initializeCommandHandlers() {
        commandHandlers.put(GameCommandType.CREATE, (sessionId, payload) -> {
            logger.info("Re-initializing game state for session: {}", sessionId);
            sessionService.initializeGame(sessionId);
        });

        commandHandlers.put(GameCommandType.PLAY, (sessionId, payload) -> {
            Optional.ofNullable(payload.get("location"))
                    .map(loc -> objectMapper.convertValue(loc, DeviceMove.class))
                    .ifPresent(move -> sessionService.updateSession(sessionId, move));
        });

        commandHandlers.put(GameCommandType.PASS, (sessionId, payload) -> 
            sessionService.updateSession(sessionId, DeviceMove.pass())
        );

        commandHandlers.put(GameCommandType.UNKNOWN, (sessionId, payload) -> 
            logger.warn("Unknown command received in update")
        );
    }

    @MessageMapping("/create")
    @SendToUser("/game/session/created")
    public SessionDto createSession(@Payload @NonNull final CreateSessionDto createSessionDto) {
        try {
            logger.info("Create session request received: {}", createSessionDto);

            final var sessionDto = sessionService.createSession(createSessionDto);
            logger.info("Session \"{}\" created", sessionDto.id);

            return sessionDto;
        } catch (InvalidUserIdException error) {
            logger.error(error.getMessage());

            return Session.invalidPlayerId(createSessionDto.getPlayerId()).toDto();
        }
    }

    @MessageMapping("/{sessionId}/terminate")
    public void terminateSession(@NonNull @DestinationVariable final String sessionId) {
        logger.info("Session \"{}\" terminated", sessionId);
        this.sessionService.terminateSession(sessionId);
    }

    @MessageMapping("/{sessionId}/update")
    @SuppressWarnings("unchecked")
    public void updateSession(@NonNull @DestinationVariable final String sessionId, @Payload @NonNull final Map<String, Object> payload) {
        logger.info("Session \"{}\" received update payload: {}", sessionId, payload);

        Optional.ofNullable(payload.get("command"))
                .filter(Map.class::isInstance)
                .map(cmd -> (Map<String, Object>) cmd)
                .map(cmd -> String.valueOf(cmd.get("name")))
                .map(GameCommandType::fromString)
                .ifPresentOrElse(
                        commandType -> handleCommand(sessionId, commandType, (Map<String, Object>) payload.get("command")),
                        () -> handleLegacyPayload(sessionId, payload)
                );
    }

    private void handleCommand(String sessionId, GameCommandType commandType, Map<String, Object> commandPayload) {
        commandHandlers.getOrDefault(commandType, commandHandlers.get(GameCommandType.UNKNOWN))
                .accept(sessionId, commandPayload);
    }

    private void handleLegacyPayload(String sessionId, Map<String, Object> payload) {
        try {
            Optional.ofNullable(objectMapper.convertValue(payload, DeviceMove.class))
                    .filter(move -> move.getType() != null)
                    .ifPresent(move -> sessionService.updateSession(sessionId, move));
        } catch (Exception e) {
            logger.error("Failed to parse update payload for session {}: {}", sessionId, payload);
        }
    }

    @MessageMapping("/{sessionId}/join")
    @SendToUser("/game/session/joined")
    public SessionDto joinSession(java.security.Principal principal, @Header("simpSessionId") final String sessionIdHeader, @NonNull @DestinationVariable final String sessionId) {
        try {
            final String playerId = principal != null ? principal.getName() : sessionIdHeader;
            logger.info("Player joins the session \"{}\" (Player ID: {})", sessionId, playerId);
            final var sessionDto = this.sessionService.joinSession(playerId, sessionId);
            logger.info("Player joined the session \"{}\"", sessionId);

            return sessionDto;
        } catch (InvalidUserIdException error) {
            logger.error(error.getMessage());

            return Session.invalidPlayerId(principal != null ? principal.getName() : sessionIdHeader).toDto();
        }
    }
}