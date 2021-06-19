package com.go.server.game.message.messages;

import com.go.server.game.session.model.output.SessionDto;

public class TerminatedMessage extends Message {
    private final static String DESTINATION = "/terminated";
    private final SessionDto sessionDto;

    public TerminatedMessage(final SessionDto sessionDto) {
        super(sessionDto.id, DESTINATION);
        this.sessionDto = sessionDto;
    }

    @Override
    public Object getPayload() {
        return sessionDto;
    }
}
