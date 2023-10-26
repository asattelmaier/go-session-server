package com.go.server.game.session;

import com.go.server.game.session.model.Session;
import com.go.server.game.session.model.input.CreateSessionDto;
import com.go.server.game.session.model.output.SessionDto;
import com.go.server.user.exeption.InvalidUserIdException;
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

    public SessionWebsocketController(@NonNull final SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @MessageMapping("/create")
    @SendToUser("/game/session/created")
    public SessionDto createSession(@Payload @NonNull final CreateSessionDto createSessionDto) {
        try {
            logger.info("Create session");
            final var sessionDto = sessionService.createSession(createSessionDto);
            logger.info("Session \"" + sessionDto.id + "\" created");

            return sessionDto;
        } catch (InvalidUserIdException error) {
            logger.error(error.getMessage());

            return Session.invalidPlayerId(createSessionDto.playerId).toDto();
        }
    }

    @MessageMapping("/{sessionId}/terminate")
    public void terminateSession(@NonNull @DestinationVariable final String sessionId) {
        logger.info("Session \"" + sessionId + "\" terminated");
        this.sessionService.terminateSession(sessionId);
    }

    @MessageMapping("/{sessionId}/update")
    public void updateSession(@NonNull @DestinationVariable final String sessionId, @NonNull final GenericMessage<byte[]> message) {
        logger.info("Session \"" + sessionId + "\" updated");
        this.sessionService.updateSession(sessionId, message.getPayload());
    }

    @MessageMapping("/{sessionId}/join")
    @SendToUser("/game/session/joined")
    public SessionDto joinSession(@Header("simpSessionId") final String playerId, @NonNull @DestinationVariable final String sessionId) {
        try {
            logger.info("Player joins the session \"" + sessionId + "\"");
            final var sessionDto = this.sessionService.joinSession(playerId, sessionId);
            logger.info("Player joined the session \"" + sessionId + "\"");

            return sessionDto;
        } catch (InvalidUserIdException error) {
            logger.error(error.getMessage());

            return Session.invalidPlayerId(playerId).toDto();
        }
    }
}