package com.go.server.game.message.messages;

import org.springframework.web.util.UriComponentsBuilder;

import static com.go.server.configuration.SessionConfig.DESTINATION_PREFIX;

public class Message {
    final String sessionId;
    final String destination;

    protected Message(final String sessionId, final String destination) {
        this.sessionId = sessionId;
        this.destination = destination;
    }

    public String getDestination() {
        return UriComponentsBuilder
                .fromPath(DESTINATION_PREFIX)
                .pathSegment(sessionId)
                .path(destination)
                .build()
                .toUriString();
    }

    public Object getPayload() {
        return new byte[0];
    }
}
