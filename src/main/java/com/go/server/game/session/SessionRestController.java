package com.go.server.game.session;

import com.go.server.configuration.WebSocketConfigConstants;
import com.go.server.game.session.model.output.SessionDto;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(WebSocketConfigConstants.DESTINATION_PREFIX)
public class SessionRestController {
    private final SessionService sessionService;

    public SessionRestController(@NonNull final SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/pending")
    public List<SessionDto> getPendingSessions() {
        return this.sessionService.getPendingSessions();
    }
}