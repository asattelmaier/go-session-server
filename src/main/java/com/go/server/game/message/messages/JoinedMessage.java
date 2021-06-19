package com.go.server.game.message.messages;

import com.go.server.game.session.model.output.SessionDto;

public class JoinedMessage extends Message {
    private final static String DESTINATION = "/player-joined";
    private final SessionDto sessionDto;

    public JoinedMessage(final SessionDto sessionDto) {
        super(sessionDto.id, DESTINATION);
        this.sessionDto = sessionDto;
    }

    @Override
    public Object getPayload() {
        return sessionDto;
    }
}
