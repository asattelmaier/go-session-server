package com.go.server.game.session;

import com.go.server.game.session.model.output.SessionDto;
import org.springframework.lang.NonNull;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Controller;

@Controller
public class SessionWebsocketController {
    private final SessionService sessionService;

    public SessionWebsocketController(@NonNull final SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @MessageMapping("/create")
    @SendToUser("/game/session/created")
    public SessionDto createSession(@Header("simpSessionId") final String playerId) {
        return this.sessionService.createSession(playerId);
    }

    @MessageMapping("/{sessionId}/terminate")
    public void terminateSession(@NonNull @DestinationVariable final String sessionId) {
        this.sessionService.terminateSession(sessionId);
    }

    @MessageMapping("/{sessionId}/update")
    public void updateSession(@NonNull @DestinationVariable final String sessionId, @NonNull final GenericMessage<byte[]> message) {
        this.sessionService.updateSession(sessionId, message.getPayload());
    }

    @MessageMapping("/{sessionId}/join")
    @SendToUser("/game/session/joined")
    public SessionDto joinSession(@Header("simpSessionId") final String playerId, @NonNull @DestinationVariable final String sessionId) {
        return this.sessionService.joinSession(playerId, sessionId);
    }
}